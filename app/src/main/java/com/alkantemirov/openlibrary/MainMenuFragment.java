package com.alkantemirov.openlibrary;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SearchView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.alkantemirov.openlibrary.databinding.FragmentMainMenuBinding;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainMenuFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private FragmentMainMenuBinding binding;
    private RecyclerView booksRecyclerView;
    private BookViewModel bookViewModel;
    private SwipeRefreshLayout swiper;
    private Dialog uploadDialog;
    private Book bookToUpload;
    private SearchView search;
    private List<BookImage> booksList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMainMenuBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initList();
        fetchBooks();
        initSwiper();
        bindUploadButton();
        bindSearch();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    @Override
    public void onRefresh() {
        new Thread(() -> {
            clearImages();

            DBFetcher fetcher = new DBFetcher();
            Thread fetch = new Thread(fetcher);
            fetch.start();
            try {
                fetch.join();
            } catch (InterruptedException ie) {
                Log.e(this.getTag(), "err: " + ie);
            }

            getActivity().runOnUiThread(() -> {
                swiper.setRefreshing(false);
                booksList = BookImage.bookArrayList;
                notifyAdapter();
            });
        }
        ).start();
    }
    private void initList() {
        booksRecyclerView = binding.booksList;
        booksList = BookImage.bookArrayList;
        bookViewModel = new ViewModelProvider(requireActivity()).get(BookViewModel.class);
        BookAdapter ba = new BookAdapter(booksList, item -> {
            NavHostFragment.findNavController(MainMenuFragment.this)
                    .navigate(R.id.action_mainMenuFragment_to_bookInfoFragment);
            bookViewModel.setBook(item);
        });
        booksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        booksRecyclerView.setAdapter(ba);
    }
    private void initSwiper() {
        swiper = binding.mainMenuSwiper;
        swiper.setOnRefreshListener(this);
    }
    private void bindSearch() {
        search = binding.mainMenuSearch;
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String query) {
                List<BookImage> filteredBooks = filter(BookImage.bookArrayList, query);
                ((BookAdapter)booksRecyclerView.getAdapter()).replaceAll(filteredBooks);
                notifyAdapter();
                booksRecyclerView.scrollToPosition(0);
                return false;
            }
        });
        search.setOnCloseListener(() -> {
            booksList = BookImage.bookArrayList;
            notifyAdapter();
            return false;
        });
    }
    private static List<BookImage> filter(List<BookImage> books, String query) {
        String lowerCaseQuery = query.toLowerCase();
        List<BookImage> filteredBookList = new ArrayList<>();
        for (BookImage book : books) {
            if (book.getTitle().toLowerCase().contains(lowerCaseQuery) ||
                    book.getAuthor().getName().toLowerCase().contains(lowerCaseQuery)) {
                filteredBookList.add(book);
            }
        }
        return filteredBookList;
    }
    private void fetchBooks() {
        DBFetcher fetcher = new DBFetcher();
        Thread fetch = new Thread(fetcher);
        fetch.start();

        try {
            fetch.join();
        } catch (InterruptedException ie) {
            Log.e(this.getTag(), "err: " + ie);
        }

        if (fetcher.getCode() == 503)
            NavHostFragment.findNavController(MainMenuFragment.this).navigate(R.id.action_mainMenuFragment_to_serverUnavailableFragment);

        booksList = BookImage.bookArrayList;
        notifyAdapter();
    }
    private void clearImages() {
        BookImage.bookArrayList.clear();
        AuthorImage.authorArrayList.clear();
    }
    private void notifyAdapter() {
        BookAdapter adapter = (BookAdapter) booksRecyclerView.getAdapter();
        synchronized (booksRecyclerView.getAdapter()) {
            adapter.notifyDataSetChanged();
        }
    }
    private void bindUploadButton() {
        uploadDialog = new Dialog(getContext());
        binding.uploadBookButton.setOnClickListener(view -> {
            bookToUpload = new Book();
            uploadDialog.setContentView(R.layout.dialog_upload_book);
            uploadDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            uploadDialog.setCancelable(true);
            uploadDialog.show();
            uploadDialog.findViewById(R.id.uploadBookCoverImage).setOnClickListener(viewview -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/png");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent = Intent.createChooser(intent, "Выберите обложку (в формате png)");
                loadCoverActivityResultLauncher.launch(intent);
            });
            uploadDialog.findViewById(R.id.uploadBookTxtButton).setOnClickListener(viewview -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("text/plain");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent = Intent.createChooser(intent, "Выберите книгу (в формате txt)");
                loadTxtActivityResultLauncher.launch(intent);
            });
            uploadDialog.findViewById(R.id.uploadBookSubmitButton).setOnClickListener(viewview -> {
                EditText title = uploadDialog.findViewById(R.id.uploadBookTitleField);
                EditText author = uploadDialog.findViewById(R.id.uploadBookAuthorField);
                String titleVal = title.getText().toString();
                String authorVal = author.getText().toString();
                uploadToServer(titleVal, authorVal);
                uploadDialog.dismiss();
                clearImages();
                fetchBooks();
                bookToUpload = null;
            });
            uploadDialog.setOnDismissListener(viewview -> bookToUpload = null);
        });
    }
    private ActivityResultLauncher<Intent> loadCoverActivityResultLauncher = registerForActivityResult (
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                File f = getCoverFromUri(requireContext().getContentResolver(), data.getData(), requireContext().getCacheDir());
                ImageView iv = uploadDialog.findViewById(R.id.uploadBookCoverImage);
                Bitmap bm = BitmapFactory.decodeFile(f.getPath());
                Bitmap bmCover = Bitmap.createScaledBitmap(bm, BookImage.coverWidth, BookImage.coverHeight, false);
                Bitmap bmThumb = Bitmap.createScaledBitmap(bm, BookImage.thumbnailWidth, BookImage.thumbnailHeight, false);
                bookToUpload.setCover(bmCover);
                bookToUpload.setThumbnail(bmThumb);
                iv.setImageBitmap(bmCover);
                clearTmpFiles(f);
            }
        }
    );
    private ActivityResultLauncher<Intent> loadTxtActivityResultLauncher = registerForActivityResult (
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                if (bookToUpload.getTxtfile() != null)
                    clearTmpFiles(bookToUpload.getTxtfile());
                Intent data = result.getData();
                File f = getTxtFromUri(requireContext().getContentResolver(), data.getData(), requireContext().getCacheDir());
                bookToUpload.setTxtFile(f);
            }
        }
    );
    private File getCoverFromUri(ContentResolver contentResolver, Uri uri, File directory) {
        try {
            File file = File.createTempFile("bk_", ".png", directory);
            OutputStream outputStream = new FileOutputStream(file);
            InputStream inputStream = contentResolver.openInputStream(uri);
            if (inputStream != null) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            inputStream.close();
            outputStream.flush();
            outputStream.close();
            return file;
        } catch (IOException ioe) {
            Log.e(this.getTag(), "err: " + ioe);
        }
        return null;
    }
    private File getTxtFromUri(ContentResolver contentResolver, Uri uri, File directory) {
        try {
            File file = File.createTempFile("bk_", ".txt", directory);
            OutputStream outputStream = new FileOutputStream(file);
            InputStream inputStream = contentResolver.openInputStream(uri);
            if (inputStream != null) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            inputStream.close();
            outputStream.flush();
            outputStream.close();
            return file;
        } catch (IOException ioe) {
            Log.e(this.getTag(), "err: " + ioe);
        }
        return null;
    }
    private void uploadToServer(String title, String author) {
        File fCover = null;
        File fThumb = null;
        if (bookToUpload.getCover() != null) {
            fCover = bitmapToFile(bookToUpload.getCover(), requireContext().getCacheDir());
            fThumb = bitmapToFile(bookToUpload.getThumbnail(), requireContext().getCacheDir());
        }
        Thread upload = new Thread(new DBUploader(fCover, fThumb, bookToUpload.getTxtfile(), title, author));
        upload.start();

        try {
            upload.join();
        } catch (InterruptedException ie) {
            Log.e(this.getTag(), "err:" + ie);
        }
        if (fCover != null && fThumb != null)
            clearTmpFiles(fCover, fThumb);
        if (bookToUpload.getTxtfile() != null)
            clearTmpFiles(bookToUpload.getTxtfile());
    }
    private File bitmapToFile(Bitmap bitmap, File directory) {
        try {
            File f = File.createTempFile("bk_", ".png", directory);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
            byte[] bitmapData = bos.toByteArray();
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(bitmapData);
            fos.flush();
            fos.close();
            return f;
        } catch (IOException ioe) {
            Log.e(this.getTag(), "err:" + ioe);
        }
        return null;
    }
    private void clearTmpFiles(File... fs) {
        for (File f : fs)
            Log.i(this.getTag(), "tmp file deleted: " + f.delete());
    }
}