package com.wingufile.wingudroid2.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.wingufile.wingudroid2.BrowserActivity;
import com.wingufile.wingudroid2.ConcurrentAsyncTask;
import com.wingufile.wingudroid2.NavContext;
import com.wingufile.wingudroid2.R;
import com.wingufile.wingudroid2.SeafException;
import com.wingufile.wingudroid2.Utils;
import com.wingufile.wingudroid2.data.DataManager;
import com.wingufile.wingudroid2.data.SeafDirent;
import com.wingufile.wingudroid2.data.SeafGroup;
import com.wingufile.wingudroid2.data.SeafItem;
import com.wingufile.wingudroid2.data.SeafRepo;


public class ReposFragment extends SherlockListFragment {

    private static final String DEBUG_TAG = "ReposFragment";

    private SeafItemAdapter adapter;
    private BrowserActivity mActivity = null;

    private ListView mList;
    private TextView mEmptyView;
    private View mProgressContainer;
    private View mListContainer;
    private TextView mErrorText;

    private DataManager getDataManager() {
        return mActivity.getDataManager();
    }

    private NavContext getNavContext() {
        return mActivity.getNavContext();
    }

    public SeafItemAdapter getAdapter() {
        return adapter;
    }

    public interface OnFileSelectedListener {
        public void onFileSelected(SeafDirent fileName);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(DEBUG_TAG, "ReposFragment Attached");
        mActivity = (BrowserActivity)activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.repos_fragment, container, false);
        mList = (ListView) root.findViewById(android.R.id.list);
        mEmptyView = (TextView) root.findViewById(android.R.id.empty);
        mListContainer =  root.findViewById(R.id.listContainer);
        mErrorText = (TextView)root.findViewById(R.id.error_message);
        mProgressContainer = root.findViewById(R.id.progressContainer);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(DEBUG_TAG, "ReposFragment onActivityCreated");
        adapter = new SeafItemAdapter(mActivity);
        setListAdapter(adapter);

        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        mListContainer.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onStart() {
        Log.d(DEBUG_TAG, "ReposFragment onStart");
        super.onStart();
    }

    @Override
    public void onStop() {
        Log.d(DEBUG_TAG, "ReposFragment onStop");
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(DEBUG_TAG, "ReposFragment onResume");
        // refresh the view (loading data)
        refreshView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDetach() {
        mActivity = null;
        Log.d(DEBUG_TAG, "ReposFragment detached");
        super.onDetach();
    }

    public void refreshView() {
        refreshView(false);
    }

    public void refreshView(boolean forceRefresh) {
        if (mActivity == null)
            return;

        NavContext navContext = getNavContext();
        if (navContext.inRepo()) {
            navToDirectory();
        } else {
            navToReposView(forceRefresh);
        }
        mActivity.invalidateOptionsMenu();
    }

    public void navToReposView(boolean forceRefresh) {
        showLoading(true);

        // load repos in background
        mActivity.disableUpButton();
        ConcurrentAsyncTask.execute(new LoadTask(getDataManager(), forceRefresh));
        updateActionBarTitle();
    }

    public void navToDirectory() {
        NavContext navContext = getNavContext();
        showLoading(true);
        mActivity.enableUpButton();
        ConcurrentAsyncTask.execute(new LoadDirTask(getDataManager()),
                                    navContext.getRepoName(),
                                    navContext.getRepoID(),
                                    navContext.getDirPath());

        updateActionBarTitle();
    }

    private void updateActionBarTitle() {
        NavContext navContext = getNavContext();
        if (!navContext.inRepo()) {
            mActivity.setActionBarTitle("", "");
        } else {

            String title = navContext.getRepoName();
            String dirPath = navContext.getDirPath();
            if (dirPath.equals("/")) {
                mActivity.setActionBarTitle(title, "");
            } else {
                mActivity.setActionBarTitle(title, Utils.fileNameFromPath(dirPath));
            }
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        //Log.d(DEBUG_TAG, "click pos " + position + " id " + id);

        NavContext nav = getNavContext();
        if (nav.inRepo()) {
            SeafDirent dirent = (SeafDirent)adapter.getItem(position);
            if (dirent.isDir()) {
                String currentPath = nav.getDirPath();
                String newPath = currentPath.endsWith("/") ?
                        currentPath + dirent.name : currentPath + "/" + dirent.name;
                nav.setDir(newPath, dirent.id);
                refreshView();
            } else {
                mActivity.onFileSelected(dirent);
            }
        } else {
            SeafItem item = adapter.getItem(position);
            if (!(item instanceof SeafRepo))
                return;
            SeafRepo repo = (SeafRepo)item;
            nav.setRepoID(repo.id);
            nav.setRepoName(repo.getName());
            nav.setDir("/", repo.root);
            refreshView();
        }
    }


    private void addReposToAdapter(List<SeafRepo> repos) {
        if (repos == null)
            return;
        Map<String, List<SeafRepo>> map = Utils.groupRepos(repos);
        List<SeafRepo> personal = map.get(Utils.NOGROUP);
        SeafGroup group;
        if (personal != null) {
            group = new SeafGroup(mActivity.getResources().getString(R.string.personal));
            adapter.add(group);
            for (SeafRepo repo : personal)
                adapter.add(repo);
        }

        for (Map.Entry<String, List<SeafRepo>> entry : map.entrySet()) {
            String key = entry.getKey();
            if (!key.equals(Utils.NOGROUP)) {
                group = new SeafGroup(key);
                adapter.add(group);
                for (SeafRepo repo : entry.getValue()) {
                    adapter.add(repo);
                }
            }
        }
    }

    private class LoadTask extends AsyncTask<Void, Void, List<SeafRepo> > {
        SeafException err = null;
        DataManager dataManager;
        boolean forceRefresh;

        public LoadTask(DataManager dataManager, boolean forceRefresh) {
            this.dataManager = dataManager;
            this.forceRefresh = forceRefresh;
        }

        @Override
        protected List<SeafRepo> doInBackground(Void... params) {
            try {
                return dataManager.getRepos(forceRefresh);
            } catch (SeafException e) {
                err = e;
                return null;
            }
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(List<SeafRepo> rs) {
            if (mActivity == null)
                // this occurs if user navigation to another activity
                return;

            if (getNavContext().inRepo()) {
                // this occurs if user already navigate into a repo
                return;
            }

            if (err != null) {
                err.printStackTrace();
                Log.i(DEBUG_TAG, "failed to load repos: " + err.getMessage());
                showError(R.string.error_when_load_repos);
                return;
            }

            if (rs != null) {
                //Log.d(DEBUG_TAG, "Load repos number " + rs.size());
                adapter.clear();
                addReposToAdapter(rs);
                adapter.notifyChanged();
                showLoading(false);
            } else {
                Log.i(DEBUG_TAG, "failed to load repos");
                showError(R.string.error_when_load_repos);
            }
        }

    }

    private void showError(int strID) {
        showError(mActivity.getResources().getString(strID));
    }

    private void showError(String msg) {
        mProgressContainer.setVisibility(View.GONE);
        mListContainer.setVisibility(View.GONE);

        adapter.clear();
        adapter.notifyChanged();

        mErrorText.setText(msg);
        mErrorText.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean show) {
        mErrorText.setVisibility(View.GONE);
        if (show) {
            mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                    mActivity, android.R.anim.fade_in));
            mListContainer.startAnimation(AnimationUtils.loadAnimation(
                    mActivity, android.R.anim.fade_out));

            mProgressContainer.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.INVISIBLE);
        } else {
            mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        mActivity, android.R.anim.fade_out));
            mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        mActivity, android.R.anim.fade_in));

            mProgressContainer.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
        }
    }

    private class LoadDirTask extends AsyncTask<String, Void, List<SeafDirent> > {

        SeafException err = null;
        String myRepoName;
        String myRepoID;
        String myPath;

        DataManager dataManager;

        public LoadDirTask(DataManager dataManager) {
            this.dataManager = dataManager;
        }

        @Override
        protected List<SeafDirent> doInBackground(String... params) {
            if (params.length != 3) {
                Log.d(DEBUG_TAG, "Wrong params to LoadDirTask");
                return null;
            }

            myRepoName = params[0];
            myRepoID = params[1];
            myPath = params[2];
            try {
                return dataManager.getDirents(myRepoID, myPath);
            } catch (SeafException e) {
                err = e;
                return null;
            }

        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(List<SeafDirent> dirents) {
            if (mActivity == null)
                // this occurs if user navigation to another activity
                return;

            NavContext nav = mActivity.getNavContext();
            if (!myRepoID.equals(nav.getRepoID()) || !myPath.equals(nav.getDirPath())) {
                return;
            }

            if (err != null) {
                if (err.getCode() == 440) {
                    showPasswordDialog();
                } else if (err.getCode() == 404) {
                    mActivity.showToast(String.format("The folder \"%s\" was deleted", myPath));
                } else {
                    Log.d(DEBUG_TAG, "failed to load dirents: " + err.getMessage());
                    err.printStackTrace();
                    showError(R.string.error_when_load_dirents);
                }
                return;
            }

            if (dirents == null) {
                showError(R.string.error_when_load_dirents);
                Log.i(DEBUG_TAG, "failed to load dir");
                return;
            }

            adapter.clear();
            if (dirents.size() > 0) {
                for (SeafDirent dirent : dirents) {
                    adapter.add(dirent);
                }
                scheduleThumbnailTask(myRepoName, myRepoID, myPath, dirents);
                adapter.notifyChanged();
                mList.setVisibility(View.VISIBLE);
                mEmptyView.setVisibility(View.GONE);
                mEmptyView.setText(R.string.dir_empty);
            } else {
                // Directory is empty
                mList.setVisibility(View.GONE);
                mEmptyView.setVisibility(View.VISIBLE);
            }

            showLoading(false);
        }
    }

    private void showPasswordDialog() {
        NavContext nav = mActivity.getNavContext();
        String repoName = nav.getRepoName();
        String repoID = nav.getRepoID();

        mActivity.showPasswordDialog(repoName, repoID, new TaskDialog.TaskDialogListener() {
            @Override
            public void onTaskSuccess() {
                refreshView();
            }
        });
    }

    private void scheduleThumbnailTask(String repoName, String repoID,
                                       String path, List<SeafDirent> dirents) {
        ArrayList<SeafDirent> needThumb = new ArrayList<SeafDirent>();
        for (SeafDirent dirent : dirents) {
            if (dirent.isDir())
                continue;
            if (Utils.isViewableImage(dirent.name)) {
                String p = Utils.pathJoin(path, dirent.name);
                File file = mActivity.getDataManager().getLocalRepoFile(repoName, repoID, p);
                if (file.exists()) {
                    if (file.length() > 1000000)
                        continue;

                    File thumb = DataManager.getThumbFile(dirent.id);
                    if (!thumb.exists())
                        needThumb.add(dirent);
                }
            }
        }
        if (needThumb.size() != 0) {
            ConcurrentAsyncTask.execute(new ThumbnailTask(repoName, repoID, path, needThumb));
        }
    }

    private class ThumbnailTask extends AsyncTask<Void, Void, Void > {

        List<SeafDirent> dirents;
        private String repoName;
        private String repoID;
        private String dir;

        public ThumbnailTask(String repoName, String repoID, String dir, List<SeafDirent> dirents) {
            this.dirents = dirents;
            this.repoName = repoName;
            this.repoID = repoID;
            this.dir = dir;
        }

        @Override
        protected Void doInBackground(Void... params) {
            for (SeafDirent dirent : dirents) {
                String path = Utils.pathJoin(dir, dirent.name);
                mActivity.getDataManager().calculateThumbnail(repoName, repoID, path, dirent.id);
            }
            return null;
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(Void v) {
            if (mActivity == null)
                // this occurs if user navigation to another activity
                return;

            adapter.notifyChanged();
        }

    }

}

