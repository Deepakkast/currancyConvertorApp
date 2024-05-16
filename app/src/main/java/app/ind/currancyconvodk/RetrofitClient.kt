package app.ind.currancyconvodk

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://api.frankfurter.app/"
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val currencyAPI: CurrencyAPI by lazy {
        retrofit.create(CurrencyAPI::class.java)
    }
}