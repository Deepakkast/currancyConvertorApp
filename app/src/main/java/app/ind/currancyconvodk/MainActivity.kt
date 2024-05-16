package app.ind.currancyconvodk

import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import app.ind.currancyconvodk.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.BigDecimal
import java.math.RoundingMode

class MainActivity : AppCompatActivity() {

    private var progressDialog: ProgressDialog? = null
    private lateinit var binding: ActivityMainBinding

    private lateinit var countryCodeList: Array<String>
    private lateinit var countryNameList: Array<String>
    private lateinit var countryCodeAdapter: ArrayAdapter<String>

    private var fromCurrency = "USD" // Defaulting "From" currency to USD
    private var toCurrency = "INR"
    private var fromCountryPosition: Int = 0
    private var toCountryPosition: Int = 0
    private var exchangeRate: Double = 0.0

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var currencyArrayList: ArrayList<String>

    private val historyList = ArrayList<Pair<String, String>>() // Changed to list of pairs

    private lateinit var historyAdapter: HistoryAdapter

    private val debounceMillis = 1000L
    private var debounceJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("CurrencyConverterPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        // Load saved default currencies
        fromCurrency = sharedPreferences.getString("defaultFromCurrency", "USD") ?: "USD"
        toCurrency = sharedPreferences.getString("defaultToCurrency", "INR") ?: "INR"

        binding.btnSetDefaultCurrencies.setOnClickListener {
            editor.putString("defaultFromCurrency", fromCurrency)
            editor.putString("defaultToCurrency", toCurrency)
            editor.apply()
            Toast.makeText(this, "Default currencies set to $fromCurrency and $toCurrency", Toast.LENGTH_SHORT).show()
        }

        binding.btnSwap.setOnClickListener {
            val temp = binding.spFromCurrency.selectedItemPosition
            binding.spFromCurrency.setSelection(binding.spToCurrency.selectedItemPosition)
            binding.spToCurrency.setSelection(temp)
        }

        binding.TvclrHistory.setOnClickListener {
            clearHistory()
            binding.rvHistory.visibility = View.GONE
            binding.TvclrHistory.visibility = View.GONE
            binding.TvHistory.visibility = View.GONE
        }

        initialiseStringArrays()

        currencyArrayList = ArrayList()
        loadArrayList()
        countryCodeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, currencyArrayList)
        binding.spFromCurrency.adapter = countryCodeAdapter
        binding.spToCurrency.adapter = countryCodeAdapter

        binding.spFromCurrency.setSelection(getIndex(CurrencyArrays.countryCodeList, fromCurrency))
        binding.spToCurrency.setSelection(getIndex(CurrencyArrays.countryCodeList, toCurrency))

        fromCountryPosition = binding.spFromCurrency.selectedItemPosition
        toCountryPosition = binding.spToCurrency.selectedItemPosition

        fromCurrency = countryCodeList[fromCountryPosition]
        toCurrency = countryCodeList[toCountryPosition]

        binding.lblFromCurrency.text = fromCurrency
        binding.lblToCurrency.text = toCurrency

        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("Please wait")
        progressDialog!!.setMessage("Getting currency conversion data")
        progressDialog!!.setCanceledOnTouchOutside(false)

        if (isInternetConnected()) {
            progressDialog!!.show()
            getExchangeRateData()
        } else {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
        }

        binding.spFromCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                fromCountryPosition = position
                toCountryPosition = binding.spToCurrency.selectedItemPosition

                fromCurrency = countryCodeList[position]
                toCurrency = countryCodeList[binding.spToCurrency.selectedItemPosition]

                binding.lblFromCurrency.text = fromCurrency
                binding.lblToCurrency.text = toCurrency

                if (position == binding.spToCurrency.selectedItemPosition) {
                    binding.txtToCurrency.text = binding.txtFromCurrency.text.toString()
                    val output = "1 $fromCurrency = 1 $toCurrency"

                } else {
                    if (isInternetConnected()) {
                        getExchangeRateData()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spToCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                fromCountryPosition = binding.spFromCurrency.selectedItemPosition
                toCountryPosition = position

                toCurrency = countryCodeList[toCountryPosition]
                fromCurrency = countryCodeList[fromCountryPosition]

                binding.lblFromCurrency.text = fromCurrency
                binding.lblToCurrency.text = toCurrency

                if (position == binding.spFromCurrency.selectedItemPosition) {
                    binding.txtToCurrency.text = binding.txtFromCurrency.text.toString()
                } else {
                    if (isInternetConnected()) {
                        getExchangeRateData()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.txtFromCurrency.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Cancel any existing debounce coroutine
                debounceJob?.cancel()

                // Start a new coroutine with debounce delay
                debounceJob = coroutineScope.launch {
                    // Delay for debounceMillis
                    delay(debounceMillis)
                    // Perform the task after the delay
                    if (!binding.txtFromCurrency.text.toString().isEmpty()) {
                        fromCountryPosition = binding.spFromCurrency.selectedItemPosition
                        toCountryPosition = binding.spToCurrency.selectedItemPosition

                        fromCurrency = countryCodeList[fromCountryPosition]
                        toCurrency = countryCodeList[toCountryPosition]

                        binding.lblFromCurrency.text = fromCurrency
                        binding.lblToCurrency.text = toCurrency

                        if (fromCountryPosition == toCountryPosition) {
                            binding.txtToCurrency.text = binding.txtFromCurrency.text.toString()
                            val output = "1 $fromCurrency = 1 $toCurrency"
                        } else {
                            if (isInternetConnected()) {
                                getExchangeRateData()
                            }
                        }
                    } else {
                        binding.txtToCurrency.text = ""
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        // RecyclerView setup
        historyAdapter = HistoryAdapter(historyList)
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = historyAdapter
        loadHistory()
    }

    private fun loadArrayList() {
        currencyArrayList.clear()

        for (i in countryCodeList.indices) {
            currencyArrayList.add("${countryNameList[i]} (${countryCodeList[i]})")
        }
    }

    fun getExchangeRateData() {
        val currencyAPICall = RetrofitClient.currencyAPI.getExchangeRate(fromCurrency, toCurrency)
        currencyAPICall.enqueue(object : Callback<ExchangeRateResponse> {
            override fun onResponse(call: Call<ExchangeRateResponse>, response: Response<ExchangeRateResponse>) {
                if (response.isSuccessful) {
                    val exchangeRateResponse = response.body()
                    exchangeRate = exchangeRateResponse?.rates?.get(toCurrency) ?: 0.0
                    progressDialog?.dismiss()

                    if (exchangeRate != 0.0 && !binding.txtFromCurrency.text.toString().isEmpty()) {
                        val input = binding.txtFromCurrency.text.toString().toDouble()
                        val result = BigDecimal.valueOf(input * exchangeRate)
                        val fromAmount = binding.txtFromCurrency.text.toString().toDouble()
                        val toAmount = BigDecimal(fromAmount * exchangeRate).setScale(2, RoundingMode.HALF_EVEN).toDouble()
                        binding.txtToCurrency.text = result.setScale(3, RoundingMode.UP).toPlainString()

                        // Add to history
                        val fromTo = "$fromCurrency to $toCurrency"
                        val result1 = "$fromAmount to $toAmount"
                        historyList.add(0, Pair(fromTo, result1))
                        historyAdapter.notifyItemInserted(0)
                        binding.rvHistory.scrollToPosition(0)
                        saveHistory()

                    } else {
                        Toast.makeText(this@MainActivity, "GET FAILED", Toast.LENGTH_SHORT).show()
                        progressDialog?.dismiss()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "GET FAILED", Toast.LENGTH_SHORT).show()
                    progressDialog?.dismiss()
                }
            }

            override fun onFailure(call: Call<ExchangeRateResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "GET FAILED", Toast.LENGTH_SHORT).show()
                progressDialog?.dismiss()
            }
        })
    }

    private fun isInternetConnected(): Boolean {
        val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun getIndex(array: Array<String>, value: String): Int {
        for (i in array.indices) {
            if (array[i] == value) {
                return i
            }
        }
        return 0
    }

    private fun initialiseStringArrays() {
        countryCodeList = arrayOf(
            "AUD", "BGN", "BRL", "CAD",
            "CHF", "CNY", "CZK", "DKK",
            "EUR", "GBP", "HKD", "HUF",
            "IDR", "ILS", "INR", "ISK",
            "JPY", "KRW", "MXN", "MYR",
            "NOK", "NZD", "PHP", "PLN",
            "RON", "SEK", "SGD", "THB",
            "TRY", "USD", "ZAR"
        )

        countryNameList = arrayOf(
            "Australian Dollar", "Bulgarian Lev", "Brazilian Real", "Canadian Dollar",
            "Swiss Franc", "Chinese Yuan", "Czech Koruna", "Danish Krone",
            "Euro", "British Pound", "Hong Kong Dollar", "Hungarian Forint",
            "Indonesian Rupiah", "Israeli New Sheqel", "Indian Rupee", "Icelandic Króna",
            "Japanese Yen", "South Korean Won", "Mexican Peso", "Malaysian Ringgit",
            "Norwegian Krone", "New Zealand Dollar", "Philippine Peso", "Polish Zloty",
            "Romanian Leu", "Swedish Krona", "Singapore Dollar", "Thai Baht",
            "Turkish Lira", "US Dollar", "South African Rand"
        )
    }

    private fun loadHistory() {
        val historyJson = sharedPreferences.getString("history", null)
        historyJson?.let {
            val type = object : TypeToken<ArrayList<Pair<String, String>>>() {}.type
            val loadedHistory = Gson().fromJson<ArrayList<Pair<String, String>>>(it, type)
            historyList.clear()
            historyList.addAll(loadedHistory)
            historyAdapter.notifyDataSetChanged()
        }
    }

    private fun saveHistory() {
        val historyJson = Gson().toJson(historyList)
        editor.putString("history", historyJson)
        editor.apply()
    }

    private fun clearHistory(){
        historyList.clear()
        historyAdapter.notifyDataSetChanged()

        // Clear history from SharedPreferences
        editor.remove("history")
        editor.apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        debounceJob?.cancel()
        saveHistory()
    }
}
