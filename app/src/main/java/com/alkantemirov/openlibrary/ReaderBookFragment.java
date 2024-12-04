package com.alkantemirov.openlibrary;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.alkantemirov.openlibrary.databinding.FragmentReaderBookBinding;

public class ReaderBookFragment extends Fragment {
    private FragmentReaderBookBinding binding;
    private BookViewModel bookViewModel;
    private boolean isEnd;

    public ReaderBookFragment() {}
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentReaderBookBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setup();
        render();
        fetchText();
        bindNavButtons();
        hideInactiveButtons();
    }
    private void setup() {
        bookViewModel = new ViewModelProvider(requireActivity()).get(BookViewModel.class);
    }
    private void render() {
        BookImage book = bookViewModel.getBook();
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(book.getTitle());
    }
    private void hideInactiveButtons() {
        if (PreferencesManager.getInstance().getValue(bookViewModel.getBook().getLocation()) <= 0)
            binding.prevPageButton.setVisibility(View.GONE);
        else
            binding.prevPageButton.setVisibility(View.VISIBLE);
        if (isEnd)
            binding.nextPageButton.setVisibility(View.GONE);
        else
            binding.nextPageButton.setVisibility(View.VISIBLE);
    }
    private void fetchText() {
        DBReaderFetcher fetcher = new DBReaderFetcher(bookViewModel.getBook().getLocation());
        Thread fetch = new Thread(fetcher);
        fetch.start();

        try {
            fetch.join();
        } catch (InterruptedException ie) {
            Log.e(this.getTag(), "err: " + ie);
        }

        if (fetcher.getCode() == 503)
            NavHostFragment.findNavController(ReaderBookFragment.this).navigate(R.id.action_readerBookFragment_to_serverUnavailableFragment);

        binding.bookText.setText(fetcher.getResult());

        isEnd = fetcher.isEndReached();
        long pgno = PreferencesManager.getInstance().getValue(bookViewModel.getBook().getLocation()) + 1;
        binding.pageNo.setText(String.valueOf(pgno));
    }
    private void bindNavButtons() {
        binding.nextPageButton.setOnClickListener(view -> {
            PreferencesManager.getInstance().setValue(bookViewModel.getBook().getLocation(),
                    PreferencesManager.getInstance().getValue(bookViewModel.getBook().getLocation()) + 1);
            fetchText();
            binding.bookTextScroller.scrollTo(0, 0);
            hideInactiveButtons();
        });
        binding.prevPageButton.setOnClickListener(view -> {
            PreferencesManager.getInstance().setValue(bookViewModel.getBook().getLocation(),
                    PreferencesManager.getInstance().getValue(bookViewModel.getBook().getLocation()) - 1);
            fetchText();
            binding.bookTextScroller.scrollTo(0, 0);
            hideInactiveButtons();
        });
    }
}