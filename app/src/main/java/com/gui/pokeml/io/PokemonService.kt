package com.gui.pokeml.io

import com.gui.pokeml.io.model.Generation
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface PokemonService {
    @GET("generation/{generation}")
    fun listRepos(@Path("generation") generation: Int): Call<Generation>
    //https://pokeapi.co/api/v2/
}