package ru.bartwell.exfilepicker.ui.adapter.holder;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;

import ru.bartwell.exfilepicker.R;
import ru.bartwell.exfilepicker.ui.callback.OnListItemClickListener;
import ru.bartwell.exfilepicker.utils.Utils;

/**
 * Created by BArtWell on 26.02.2017.
 */
public class FileFilesListHolder extends BaseFilesListHolder {
    @NonNull
    private final Context mContext;
    @Nullable
    private final AppCompatTextView mFileSize;
    @NonNull
    private final AppCompatImageView mThumbnail;

    public FileFilesListHolder(@NonNull View itemView) {
        super(itemView);
        mContext = itemView.getContext();
        mFileSize = itemView.findViewById(R.id.filesize);
        mThumbnail = itemView.findViewById(R.id.thumbnail);
    }

    @Override
    public void bind(@NonNull File file, boolean isMultiChoiceModeEnabled, boolean isSelected, @Nullable OnListItemClickListener listener) {
        super.bind(file, isMultiChoiceModeEnabled, isSelected, listener);
        if (mFileSize != null) {
            mFileSize.setVisibility(View.VISIBLE);
            mFileSize.setText(Utils.getHumanReadableFileSize(mContext, file.length()));
        }

        RequestOptions options = new RequestOptions()
                .error(R.drawable.efp__ic_file);
        Glide.with(mContext)
                .load(file)
                .apply(options)
                .into(mThumbnail);
    }
}
