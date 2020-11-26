package com.gui.pokeml.io.model

data class Generation (
        val id: Int,
        val name: String,
        val names: List<Language>,
//        val abilities: List<Item>,
        val main_region: Item,
        val moves: List<Item>,
        val pokemon_species: List<Item>,
        val types: List<Item>,
        val version_groups: List<Item>,
)