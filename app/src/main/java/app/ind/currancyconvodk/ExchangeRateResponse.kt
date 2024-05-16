package app.ind.currancyconvodk

import com.google.gson.annotations.SerializedName

data class ExchangeRateResponse(@SerializedName("rates") val rates: Map<String, Double>,
                                @SerializedName("date") val date: String)
