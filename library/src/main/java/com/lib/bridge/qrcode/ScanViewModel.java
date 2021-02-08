package com.lib.bridge.qrcode;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.annotation.NonNull;

import java.util.List;

public class ScanViewModel extends AndroidViewModel {
    private ScanRepository repository;
    private LiveData<List<Scan>> allScans;

    public ScanViewModel(@NonNull Application application) {
        super(application);
        repository = new ScanRepository(application);
        allScans = repository.getAllScans();
    }

    public void insert(Scan note) {
        repository.insert(note);
    }

    public void update(Scan note) {
        repository.update(note);
    }

    public void delete(Scan note) {
        repository.delete(note);
    }

    public void deleteAllScans() {
        repository.deleteAllScans();
    }

    public LiveData<List<Scan>> getAllScans() {
        return allScans;
    }
}