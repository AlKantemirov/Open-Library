package com.alkantemirov.openlibrary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import java.util.List;
import android.view.LayoutInflater;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {
    private List<BookImage> books;
    private final OnItemClickListener listener;

    public BookAdapter(List<BookImage> books, OnItemClickListener listener) {
        this.books = books;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.book_menu_item, parent, false);
        return new BookViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        BookImage book = books.get(position);
        holder.bind(book, listener);
        StringBuilder prevpath = new StringBuilder();
        prevpath.append(Configuration.PREVIEWS_DIR);
        prevpath.append(book.getPreview());
        holder.getTitle().setText(book.getTitle());
        holder.getAuthor().setText(book.getAuthor().getName());
        fetchPreview(prevpath.toString(), holder.getPreview());
    }
    @Override
    public int getItemCount() {
        return books.size();
    }
    public void replaceAll(List<BookImage> newBooks) {
        books = newBooks;
    }
    private void fetchPreview(String src, ImageView iv) {
        Glide.with(iv.getContext())
                .asBitmap()
                .load(src)
                .addListener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<Bitmap> target, boolean isFirstResource) {
                        try {
                            Bitmap placeholder = BitmapFactory.decodeResource(iv.getResources(), R.drawable.blank);
                            iv.setImageBitmap(placeholder);
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
                        iv.setImageBitmap(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }
    public static class BookViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView author;
        private final ImageView preview;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.bookTitle);
            author = itemView.findViewById(R.id.bookAuthor);
            preview = itemView.findViewById(R.id.bookPreview);
        }
        public void bind(final BookImage item, final OnItemClickListener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    listener.onItemClick(item);
                }
            });
        }
        public TextView getTitle() {return title;}
        public TextView getAuthor() {return author;}
        public ImageView getPreview() {return preview;}
    }
    public interface OnItemClickListener {
        void onItemClick(BookImage item);
    }
}
