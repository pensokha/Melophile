package com.vpaliy.melophile.ui.search;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.vpaliy.domain.model.Playlist;
import com.vpaliy.domain.model.Track;
import com.vpaliy.domain.model.User;
import com.vpaliy.melophile.App;
import com.vpaliy.melophile.R;
import com.vpaliy.melophile.di.component.DaggerViewComponent;
import com.vpaliy.melophile.di.module.PresenterModule;
import com.vpaliy.melophile.ui.base.BaseActivity;
import com.vpaliy.melophile.ui.base.BaseAdapter;
import com.vpaliy.melophile.ui.base.bus.event.ExposeEvent;
import com.vpaliy.melophile.ui.transition.CircularReveal;
import com.vpaliy.melophile.ui.user.UserPlaylistsAdapter;
import com.vpaliy.melophile.ui.user.UserTracksAdapter;
import com.vpaliy.melophile.ui.user.info.UserAdapter;
import java.util.List;

import android.support.annotation.TransitionRes;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.text.TextUtils;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import butterknife.ButterKnife;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.SearchView;
import static com.vpaliy.melophile.ui.search.SearchContract.Presenter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import javax.inject.Inject;
import butterknife.BindView;
import butterknife.OnClick;

public class SearchActivity extends BaseActivity
            implements SearchContract.View{

    private static final String TAG=SearchActivity.class.getSimpleName();

    private Presenter presenter;
    private SearchAdapter searchAdapter;

    @BindView(R.id.search_view)
    protected SearchView searchView;

    @BindView(R.id.pager)
    protected ViewPager pager;

    @BindView(R.id.progress)
    protected ProgressBar progressBar;

    @BindView(R.id.tabs)
    protected TabLayout tabs;

    @BindView(R.id.back)
    protected View back;

    @BindView(R.id.root)
    protected ViewGroup root;

    private boolean isFocus=true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);
        setupSearch();
        setupPager();
        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.hasExtra(SearchManager.QUERY)) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (!TextUtils.isEmpty(query)) {
                Log.d(TAG,query);
                searchView.setQuery(query, false);
                presenter.query(query);
            }
        }
    }

    @OnClick(R.id.back)
    public void close(){
        back.setBackground(null);
        finishAfterTransition();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isFocus){

        }
    }

    private void setupPager(){
        searchAdapter=new SearchAdapter(getSupportFragmentManager());
        pager.setOffscreenPageLimit(3);
        pager.setAdapter(searchAdapter);
        tabs.setupWithViewPager(pager);
    }

    private void setupSearch(){
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        searchView.setImeOptions(searchView.getImeOptions() | EditorInfo.IME_ACTION_SEARCH |
                EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                presenter.query(query);
                progressBar.setVisibility(View.VISIBLE);
                hideKeyboard();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                if (TextUtils.isEmpty(query)) {
                    clear();
                }
                return true;
            }
        });
    }

    @Override
    public void inject() {
        DaggerViewComponent.builder()
                .presenterModule(new PresenterModule())
                .applicationComponent(App.appInstance().appComponent())
                .build().inject(this);
    }

    @Override
    public void handleEvent(@NonNull Object event) {
        if(event instanceof ExposeEvent){
            navigator.navigate(this,(ExposeEvent)(event));
        }
    }

    private void hideKeyboard(){
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void clear(){
        TransitionManager.beginDelayedTransition(root,getTransition(R.transition.search_hide_result));
        pager.setVisibility(View.GONE);
        tabs.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(presenter!=null){
            presenter.stop();
        }
    }

    private void gotResult(){
        progressBar.setVisibility(View.GONE);
        if(pager.getVisibility()!= View.VISIBLE){
            TransitionManager.beginDelayedTransition(root,getTransition(R.transition.search_show_result));
            tabs.setVisibility(View.VISIBLE);
            pager.setVisibility(View.VISIBLE);
        }
    }

    private Transition getTransition(@TransitionRes int transitionId) {
        TransitionInflater inflater=TransitionInflater.from(this);
        return inflater.inflateTransition(transitionId);
    }

    @Override
    @Inject
    public void attachPresenter(@NonNull Presenter presenter) {
        this.presenter=presenter;
        this.presenter.attachView(this);
    }

    @Override
    public void showTracks(@NonNull List<Track> tracks) {
        gotResult();
        UserTracksAdapter adapter=new UserTracksAdapter(this,eventBus);
        adapter.setData(tracks);
        SearchResult result=searchAdapter.getItem(0);
        if(result!=null){
            result.setAdapter(adapter);
        }
    }

    @Override
    public void showPlaylists(@NonNull List<Playlist> playlists) {
        gotResult();
        UserPlaylistsAdapter adapter=new UserPlaylistsAdapter(this,eventBus);
        adapter.setData(playlists);
        setResultAdapter(adapter,1);
    }

    @Override
    public void showUsers(@NonNull List<User> users) {
        gotResult();
        UserAdapter adapter=new UserAdapter(this,eventBus);
        adapter.setData(users);
        setResultAdapter(adapter,2);
    }

    private void setResultAdapter(BaseAdapter<?> adapter, int position){
        SearchResult result=searchAdapter.getItem(position);
        if(result!=null){
            result.setAdapter(adapter);
        }
    }

    @Override
    public void showEmptyMessage() {
        //TODO add empty message
    }

    @Override
    public void showErrorMessage() {
        //TODO add error message
    }
}
