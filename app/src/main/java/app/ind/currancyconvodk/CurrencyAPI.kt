package app.ind.currancyconvodk

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface CurrencyAPI {
    @GET("latest")
    fun getExchangeRate(
        @Query("from") fromCurrency: String,
        @Query("to") toCurrency: String
    ): Call<ExchangeRateResponse>

}