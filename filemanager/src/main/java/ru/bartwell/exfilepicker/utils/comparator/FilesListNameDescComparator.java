package ru.bartwell.exfilepicker.utils.comparator;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import java.io.File;
import java.util.Locale;

/**
 * Created by BArtWell on 28.02.2017.
 */
class FilesListNameDescComparator extends FilesListComparator {
    @Override
    @IntRange(from = -1, to = 1)
    int compareProperty(@NonNull File file1, @NonNull File file2) {
        return file2.getName().toLowerCase(Locale.getDefault()).compareTo(file1.getName().toLowerCase(Locale.getDefault()));
    }
}
