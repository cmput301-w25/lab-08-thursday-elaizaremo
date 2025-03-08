package com.example.androidcicd.movie;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QuerySnapshot;


import java.util.ArrayList;

public class MovieProvider {
    private static MovieProvider movieProvider;
    private final ArrayList<Movie> movies;
    private final CollectionReference movieCollection;

    public MovieProvider(FirebaseFirestore firestore) {
        movies = new ArrayList<>();
        movieCollection = firestore.collection("movies");
    }

    public interface DataStatus {
        void onDataUpdated();
        void onError(String error);
    }

    public void listenForUpdates(final DataStatus dataStatus) {
        movieCollection.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                dataStatus.onError(error.getMessage());
                return;
            }
            movies.clear();
            if (snapshot != null) {
                for (QueryDocumentSnapshot item : snapshot) {
                    movies.add(item.toObject(Movie.class));
                }
                dataStatus.onDataUpdated();
            }
        });
    }

    public static MovieProvider getInstance(FirebaseFirestore firestore) {
        if (movieProvider == null)
            movieProvider = new MovieProvider(firestore);
        return movieProvider;
    }

    public ArrayList<Movie> getMovies() {
        return movies;
    }

    public void updateMovie(Movie movie, String title, String genre, int year) {
        if (!isTitleUnique(movie.getTitle())) {
            throw new IllegalArgumentException("There is already a movie with this title");
        }
        movie.setTitle(title);
        movie.setGenre(genre);
        movie.setYear(year);
        DocumentReference docRef = movieCollection.document(movie.getId());
        if (validMovie(movie, docRef)) {
            docRef.set(movie);
        } else {
            throw new IllegalArgumentException("Invalid Movie!");
        }
    }

    public void addMovie(Movie movie) {
        // Check first if movie title is unique
        if (!isTitleUnique(movie.getTitle())) {
            throw new IllegalArgumentException("There is already a movie with this title");
        }
        DocumentReference docRef = movieCollection.document();
        movie.setId(docRef.getId());
        if (validMovie(movie, docRef)) {
            docRef.set(movie);
        } else {
            throw new IllegalArgumentException("Invalid Movie!");
        }
    }

    public void deleteMovie(Movie movie) {
        DocumentReference docRef = movieCollection.document(movie.getId());
        docRef.delete();
    }

    public boolean validMovie(Movie movie, DocumentReference docRef) {
        return movie.getId().equals(docRef.getId()) && !movie.getTitle().isEmpty() && !movie.getGenre().isEmpty() && movie.getYear() > 0;
    }

    public boolean isTitleUnique(String title) {
        Query query = movieCollection.whereEqualTo("title", title);
        Task<QuerySnapshot> queryTask = query.get();
        try {
            QuerySnapshot snapshot = queryTask.getResult();
            return snapshot.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
