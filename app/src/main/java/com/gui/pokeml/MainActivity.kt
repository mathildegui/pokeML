package com.gui.pokeml

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.custom.FirebaseCustomRemoteModel
import com.gui.pokeml.databinding.ActivityMainBinding
import com.gui.pokeml.io.PokemonService
import com.gui.pokeml.io.model.Generation
import org.tensorflow.lite.Interpreter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var remoteModel : FirebaseCustomRemoteModel? = null
    private var interpreter : Interpreter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        imgData.order(ByteOrder.nativeOrder())
        launchModel()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            dispatchTakePictureIntent()
        }
        fetch()
    }

    fun fetch() {
        val retrofit: Retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl("https://pokeapi.co/api/v2/")
                .build()
        val service: PokemonService = retrofit.create(PokemonService::class.java)
        service.listRepos(1).enqueue(object: Callback<Generation> {
            override fun onFailure(call: Call<Generation>, t: Throwable) {
                Log.d(TAG, "onFailure")
                t.printStackTrace()
            }

            override fun onResponse(call: Call<Generation>, response: Response<Generation>) {
                if(response.body() == null) {
                    Log.d(TAG, "onResponse: Empty body")
//                    callback.onException(AriseException(context.getString(R.string.arise_error_wrong_access_key)))
                }

                response.body()?.let {
                    Log.d(TAG, "onResponse: $it")
//                    ArisePreferencesUtil(context).set(
//                            ArisePreferencesUtil.PREF_STADIUM_ID,
//                            it.get("client_id").asString
//                    )
//                    callback.onComplete()
                }
            }
        })
//        Call<List<Pokemon>> pokemons = service.listRepos(1)
    }

    val REQUEST_IMAGE_CAPTURE = 1

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {
            // display error state to the user
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            //imageView.setImageBitmap(imageBitmap)
            if(imageBitmap != null) {
                val scaledBitmap = Bitmap.createScaledBitmap(imageBitmap, 224, 224, true)
                val input = ByteBuffer.allocateDirect(224 * 224 * 3 * 4).order(ByteOrder.nativeOrder())
                for (y in 0 until DIM_IMG_SIZE_Y) {
                    for (x in 0 until DIM_IMG_SIZE_X) {
                        val px = scaledBitmap.getPixel(x, y)

                        // Get channel values from the pixel value.
                        val r = Color.red(px)
                        val g = Color.green(px)
                        val b = Color.blue(px)

                        // Normalize channel values to [-1.0, 1.0]. This requirement depends on the model.
                        // For example, some models might require values to be normalized to the range
                        // [0.0, 1.0] instead.
                        val rf = (r - IMAGE_MEAN) / IMAGE_STD
                        val gf = (g - IMAGE_MEAN) / IMAGE_STD
                        val bf = (b - IMAGE_MEAN) / IMAGE_STD

                        input.putFloat(rf)
                        input.putFloat(gf)
                        input.putFloat(bf)
                    }
                }
                val bufferSize = 149 * java.lang.Float.SIZE / java.lang.Byte.SIZE
                val modelOutput = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())
                interpreter?.run(input, modelOutput).let {
                    modelOutput.rewind()
                    val probabilities = modelOutput.asFloatBuffer()
                    try {
                        for (i in 0 until probabilities.capacity()) {
                            val label: String = pokeArray[i]
                            val probability = probabilities.get(i)
                            if(probability > 0.5)
                                Log.d("POKE_RESULT", "$label: $probability")
                        }
                    } catch (e: IOException) {
                        // File not found?
                    }
                }

//                convertBitmapToByteBuffer(scaledBitmap)
//                transformImg()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    fun toto() {
        val remoteModel = FirebaseCustomRemoteModel.Builder("your_model").build()
        FirebaseModelManager.getInstance().getLatestModelFile(remoteModel)
                .addOnCompleteListener { task ->
                    val modelFile = task.result
                    if (modelFile != null) {
                        interpreter = Interpreter(modelFile)
                    } else {

                    }
                }
    }

    fun launchModel() {
        getLatestModelFile()
        /*remoteModel = FirebaseCustomRemoteModel.Builder("pokemon-first-gen").build()
        val conditions = FirebaseModelDownloadConditions.Builder()
                .requireWifi()
                .build()
        remoteModel?.let {
            FirebaseModelManager.getInstance().download(it, conditions)
                    .addOnCompleteListener {
                        Log.d("FirebaseModelManager: Success", "Yo")
                        // Download complete. Depending on your app, you could enable the ML
                        // feature, or switch from the local model to the remote model, etc.
                        getLatestModelFile()
                    }.addOnFailureListener {
                        Log.d("FirebaseModelManager: Failure", it.message!!)
                    }
        }*/
    }

    private fun getLatestModelFile() {
        val model = assets.open("pokedex_91.tflite").readBytes()
        val buffer = ByteBuffer.allocateDirect(model.size).order(ByteOrder.nativeOrder())
        buffer.put(model)
        interpreter = Interpreter(buffer)
        Log.d("FirebaseModelManager: Failure", "interpreter")

        /*remoteModel?.let {
            FirebaseModelManager.getInstance().getLatestModelFile(it)
                    .addOnCompleteListener { task ->
                        val modelFile = task.result
                        if (modelFile != null) {
                            Log.d("FirebaseModelManager", "modelFile != null")
                            interpreter = Interpreter(modelFile)
                        } else {
                            Log.d("FirebaseModelManager", "modelFile == null")
                        }
                    }
        }*/
    }

    val pokeArray: Array<String> = arrayOf("abra", "aerodactyl", "alakazam", "arbok", "arcanine", "articuno", "beedrill", "bellsprout",
            "blastoise", "bulbasaur", "butterfree", "caterpie", "chansey", "charizard", "charmander", "charmeleon", "clefable", "clefairy", "cloyster", "cubone", "dewgong",
            "diglett", "ditto", "dodrio", "doduo", "dragonair", "dragonite", "dratini", "drowzee", "dugtrio", "eevee", "ekans", "electabuzz",
            "electrode", "exeggcute", "exeggutor", "farfetchd", "fearow", "flareon", "gastly", "gengar", "geodude", "gloom",
            "golbat", "goldeen", "golduck", "golem", "graveler", "grimer", "growlithe", "gyarados", "haunter", "hitmonchan",
            "hitmonlee", "horsea", "hypno", "ivysaur", "jigglypuff", "jolteon", "jynx", "kabuto",
            "kabutops", "kadabra", "kakuna", "kangaskhan", "kingler", "koffing", "krabby", "lapras", "lickitung", "machamp",
            "machoke", "machop", "magikarp", "magmar", "magnemite", "magneton", "mankey", "marowak", "meowth", "metapod",
            "mew", "mewtwo", "moltres", "mrmime", "muk", "nidoking", "nidoqueen", "nidorina", "nidorino", "ninetales",
            "oddish", "omanyte", "omastar", "onix", "paras", "parasect", "persian", "pidgeot", "pidgeotto", "pidgey",
            "pikachu", "pinsir", "poliwag", "poliwhirl", "poliwrath", "ponyta", "porygon", "primeape", "psyduck", "raichu",
            "rapidash", "raticate", "rattata", "rhydon", "rhyhorn", "sandshrew", "sandslash", "scyther", "seadra",
            "seaking", "seel", "shellder", "slowbro", "slowpoke", "snorlax", "spearow", "squirtle", "starmie", "staryu",
            "tangela", "tauros", "tentacool", "tentacruel", "vaporeon", "venomoth", "venonat", "venusaur", "victreebel",
            "vileplume", "voltorb", "vulpix", "wartortle", "weedle", "weepinbell", "weezing", "wigglytuff", "zapdos", "zubat")

    companion object {
        /** Dimensions of inputs.  */
        const val DIM_IMG_SIZE_X = 224
        const val DIM_IMG_SIZE_Y = 224
        const val DIM_BATCH_SIZE = 1
        const val DIM_PIXEL_SIZE = 3
        const val IMAGE_MEAN = 128
        private const val IMAGE_STD = 128.0f
        private const val TAG = "Poke::MainActivity"
    }
}