package com.example.coroutines.domain

class GetPopularMoviesUseCase(
    private val moviesRepository: MoviesRepository
) {

    fun getPopularMovies() {
        moviesRepository.getPopularMovies()
    }
}