package com.wingufile.wingudroid2.ui;

import java.util.ArrayList;
import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wingufile.wingudroid2.R;
import com.wingufile.wingudroid2.SeadroidApplication;
import com.wingufile.wingudroid2.data.SeafRepo;

public class ReposAdapter extends BaseAdapter {

    private List<SeafRepo> repos;

    public ReposAdapter() {
        repos = new ArrayList<SeafRepo>();
    }

    @Override
    public int getCount() {
        return repos.size();
    }

    @Override
    public boolean isEmpty() {
        return repos.isEmpty();
    }

    public void add(SeafRepo repo) {
        repos.add(repo);
    }

    @Override
    public SeafRepo getItem(int position) {
        return repos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setRepos(List<SeafRepo> repos) {
        this.repos.clear();
        for (SeafRepo repo: repos) {
            this.repos.add(repo);
        }
        notifyDataSetChanged();
    }

    public boolean areAllReposSelectable() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        Viewholder viewHolder;

        SeafRepo repo = repos.get(position);

        if (convertView == null) {
            view = LayoutInflater.from(SeadroidApplication.getAppContext())
                    .inflate(R.layout.list_item_entry, null);
            TextView title = (TextView) view.findViewById(R.id.list_item_title);
            TextView subtitle = (TextView) view.findViewById(R.id.list_item_subtitle);
            ImageView icon = (ImageView) view.findViewById(R.id.list_item_icon);
            ImageView action = (ImageView) view.findViewById(R.id.list_item_action);
            viewHolder = new Viewholder(title, subtitle, icon, action);
            view.setTag(viewHolder);
        } else {
            viewHolder = (Viewholder) convertView.getTag();
        }

        viewHolder.title.setText(repo.getTitle());
        viewHolder.subtitle.setText(repo.getSubtitle());
        viewHolder.icon.setImageResource(repo.getIcon());
        viewHolder.action.setVisibility(View.INVISIBLE);
        return view;
    }

    private class Viewholder {
        TextView title, subtitle;
        ImageView icon, action;

        public Viewholder(TextView title, TextView subtitle, ImageView icon, ImageView action) {
            super();
            this.icon = icon;
            this.action = action;
            this.title = title;
            this.subtitle = subtitle;
        }
    }
}

