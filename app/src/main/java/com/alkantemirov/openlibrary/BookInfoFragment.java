package com.alkantemirov.openlibrary;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import com.alkantemirov.openlibrary.databinding.FragmentBookInfoBinding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

public class BookInfoFragment extends Fragment {
    private FragmentBookInfoBinding binding;
    private BookViewModel bookViewModel;
    private Dialog updateDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBookInfoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setup();
        render();
        bindFabDelete();
        bindFabUpdate();
    }
    private void setup() {
        bookViewModel = new ViewModelProvider(requireActivity()).get(BookViewModel.class);
    }
    private void render() {
        BookImage book = bookViewModel.getBook();
        StringBuilder coverpath = new StringBuilder();
        coverpath.append(Configuration.COVERS_DIR).append(book.getPreview());
        binding.bookInfoTitle.setText(book.getTitle());
        binding.bookInfoAuthor.setText(book.getAuthor().getName());
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(bookViewModel.getBook().getTitle());
        fetchCover(coverpath.toString());
    }
    private void fetchCover(String src) {
        Glide.with(getContext())
                .asBitmap()
                .load(src)
                .addListener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<Bitmap> target, boolean isFirstResource) {
                        try {
                            Bitmap placeholder = BitmapFactory.decodeResource(binding.bookInfoPreview.getResources(), R.drawable.blank);
                            binding.bookInfoPreview.setImageBitmap(placeholder);
                        } catch (Exception ee) {
                            ee.printStackTrace();
                        }
                        return true;
                    }

                    @Override
                    public boolean onResourceReady(@NonNull Bitmap resource, @NonNull Object model, Target<Bitmap> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                        return false;
                    }
                })
                .placeholder(R.drawable.blank)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        binding.bookInfoPreview.setImageBitmap(resource);
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }});
    }
    private void bindFabDelete() {
        binding.bookInfoDelete.setOnClickListener(view -> {
            AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
            alert.setTitle("Удалить");
            alert.setMessage("Вы действительно хотите удалить \"" + bookViewModel.getBook().getTitle() + "\" безвозвратно?");
            alert.setPositiveButton(android.R.string.yes, (dialog, which) -> {
                removeBook();
                NavHostFragment.findNavController(BookInfoFragment.this)
                        .navigate(R.id.action_bookInfoFragment_to_mainMenuFragment);
                clearImages();
            });
            alert.setNegativeButton(android.R.string.no, (dialog, which) -> dialog.cancel());
            alert.show();
        });
    }
    private void bindFabUpdate() {
        binding.bookInfoUpdate.setOnClickListener(view -> {
            updateDialog = new Dialog(getContext());
            updateDialog.setContentView(R.layout.dialog_update_book);
            updateDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            updateDialog.setCancelable(true);
            EditText title = updateDialog.findViewById(R.id.updateBookTitleField);
            EditText author = updateDialog.findViewById(R.id.updateBookAuthorField);
            title.setText(bookViewModel.getBook().getTitle());
            author.setText(bookViewModel.getBook().getAuthor().getName());
            updateDialog.show();
            updateDialog.findViewById(R.id.updateBookSubmitButton).setOnClickListener(viewview -> {
                String titleVal = title.getText().toString();
                String authorVal = author.getText().toString();
                updateBook(titleVal, authorVal);
                clearImages();
                fetchBooks();
                bookViewModel.getBook().setTitle(titleVal);
                bookViewModel.getBook().setAuthorid(findAuthorByName(authorVal).getId());
                render();
                updateDialog.dismiss();
            });
        });
    }
    private void clearImages() {
        BookImage.bookArrayList.clear();
        AuthorImage.authorArrayList.clear();
    }
    private void removeBook() {
        Thread fetch = new Thread(new DBRemover(bookViewModel.getBook().getId()));
        fetch.start();

        try {
            fetch.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void updateBook(String newTitle, String newAuthor) {
        Thread fetch = new Thread(new DBUpdater(bookViewModel.getBook().getId(), newTitle, newAuthor));
        fetch.start();

        try {
            fetch.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void fetchBooks() {
        DBFetcher fetcher = new DBFetcher();
        Thread fetch = new Thread(fetcher);
        fetch.start();

        try {
            fetch.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private AuthorImage findAuthorByName(String name) {
        for (AuthorImage ai : AuthorImage.authorArrayList)
            if (ai.getName().equals(name))
                return ai;
        return null;
    }
}