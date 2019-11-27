package com.example.coroutines.presentation

import android.os.Handler
import android.os.Looper
import com.example.coroutines.data.entities.Genres
import com.example.coroutines.data.entities.Movies
import com.example.coroutines.presentation.entities.Movie
import kotlinx.coroutines.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.Executors


class Presenter(private val mainActivity: MainActivity) {


    private val tmdbService: TMDBService

    init {
        val keyInterceptor = Interceptor { chain ->
            val key = "0c00f78c7b002e5a6e91e6675b76f590"

            val original = chain.request()
            val originalHttpUrl = original.url()

            val url = originalHttpUrl.newBuilder()
                .addQueryParameter("api_key", key)
                .build()

            val requestBuilder = original.newBuilder()
                .url(url)

            val request = requestBuilder.build()
            return@Interceptor chain.proceed(request)
        }

        val okHttpClient =
            OkHttpClient.Builder()
                .addInterceptor(keyInterceptor)
                .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
        tmdbService = retrofit.create(TMDBService::class.java)
    }

    private val scope = CoroutineScope(Dispatchers.Main)

    private val executor = Executors.newSingleThreadExecutor()


    private val mainHandler = Handler(Looper.getMainLooper())

    fun start1() { // synchronous
        executor.execute {
            val movies = tmdbService.getTopRatedMovies().execute().body()!!
            val genres = tmdbService.getGenres().execute().body()!!

            val genreIdToNameMap = genres.toMap()

//            val moviesToShow = createMovies(genreIdToNameMap, movies)

//            mainHandler.post {
//                mainActivity.showMovies(moviesToShow)
//            }
        }
    }


    fun start() {
        scope.launch {
            val genres = async {
                tmdbService.getGenresSuspended()
            }

            val movies = async {
                tmdbService.getTopRatedMoviesSuspended()
            }


            val genereIdToNameMap = genres.await().toMap()

            val moviesToShow = createMovies(genereIdToNameMap, movies.await())

            mainActivity.showMovies(moviesToShow)
        }
    }

    //    fun start2() { // callbacks
//        tmdbService.getTopRatedMovies(API_KEY).enqueue(object : Callback<Movies> {
//            override fun onFailure(call: Call<Movies>, t: Throwable) {}
//
//            override fun onResponse(call: Call<Movies>, response: Response<Movies>) {
//                tmdbService.getGenres(API_KEY).enqueue(object : Callback<Genres> {
//
//                    override fun onFailure(call: Call<Genres>, t: Throwable) {
//
//                    }
//
//                    override fun onResponse(call: Call<Genres>, response: Response<Genres>) {
//
//                    }
//
//                })
//            }
//        })
//    }
//

//    suspend fun getTopRatedMovies() = suspendCoroutine<Movies> { cont ->
//        tmdbService.getTopRatedMovies(API_KEY).enqueue(object : Callback<Movies> {
//            override fun onFailure(call: Call<Movies>, t: Throwable) {
//                cont.resumeWithException(t)
//            }
//
//            override fun onResponse(call: Call<Movies>, response: Response<Movies>) {
//                if (response.isSuccessful) {
//                    cont.resume(response.body()!!)
//                } else {
//                    cont.resumeWithException(ErrorResponse(response))
//                }
//            }
//        })
//    }
//

    private suspend fun createMovies(
        generes: Map<Int, String>,
        movies: Movies
    ): MutableList<Movie> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Movie>()
        movies.results.forEach {
            list.add(
                Movie(
                    it.title,
                    it.genre_ids.map { genre -> generes.getValue(genre) }
                )
            )
        }

        return@withContext list
    }

}

interface TMDBService {
    @GET("movie/top_rated")
    fun getTopRatedMovies(): Call<Movies>

    @GET("genre/movie/list")
    fun getGenres(): Call<Genres>

    @GET("movie/top_rated")
    suspend fun getTopRatedMoviesSuspended(): Movies

    @GET("genre/movie/list")
    suspend fun getGenresSuspended(): Genres
}

private fun Genres.toMap(): Map<Int, String> {
    val map = mutableMapOf<Int, String>()
    this.genres.forEach {
        map[it.id] = it.name
    }
    return map
}


class ErrorResponse(val response: Response<Movies>) : Throwable() {

}