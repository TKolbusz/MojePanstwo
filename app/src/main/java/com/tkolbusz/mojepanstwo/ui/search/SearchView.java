package com.tkolbusz.mojepanstwo.ui.search;

import android.os.Parcelable;
import android.view.LayoutInflater;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tkolbusz.domain.exception.ConnectionException;
import com.tkolbusz.domain.exception.ProviderException;
import com.tkolbusz.domain.model.CompanySmall;
import com.tkolbusz.domain.model.QueryData;
import com.tkolbusz.domain.view.SearchContract;
import com.tkolbusz.mojepanstwo.R;
import com.tkolbusz.mojepanstwo.base.BaseView;
import com.tkolbusz.mojepanstwo.base.IMainDisplay;
import com.tkolbusz.mojepanstwo.ui.detail.DetailView;

import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("ViewConstructor")
public class SearchView extends BaseView implements SearchContract.View {
    private final RecyclerView companiesListView;
    private final androidx.appcompat.widget.SearchView androidSearchView;
    private final Toolbar toolbar;
    private final SearchListAdapter adapter;

    private final SearchController controller;

    public SearchView(@NotNull IMainDisplay mainDisplay) {
        super(mainDisplay);
        this.controller = mainDisplay.getComponent().createSearchController();
        setController(this.controller);
        LayoutInflater.from(mainDisplay.getContext()).inflate(R.layout.search_view, this, true);
        toolbar = findViewById(R.id.detail_view_toolbar);
        toolbar.setTitle(getString(R.string.search_companies_title));
        toolbar.inflateMenu(R.menu.search_menu);
        androidSearchView = ((androidx.appcompat.widget.SearchView) toolbar.getMenu().findItem(R.id.action_search).getActionView());
        androidSearchView.setMaxWidth(Integer.MAX_VALUE);
        androidSearchView.setQueryHint(getString(R.string.query_hint));
        companiesListView = findViewById(R.id.search_view_recyclerView);
        companiesListView.setLayoutManager(new LinearLayoutManager(mainDisplay.getContext()));
        companiesListView.addItemDecoration(new DividerItemDecoration(mainDisplay.getContext(), DividerItemDecoration.VERTICAL));
        adapter = new SearchListAdapter(
                companiesListView,
                company -> controller.onCompanySelected(company),
                pageToLoad -> controller.onLoadMoreData(QueryData.from(androidSearchView.getQuery().toString(), pageToLoad))
        );
        companiesListView.setAdapter(adapter);


        androidSearchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                controller.onNewSearchQuery(QueryData.from(newText));
                return true;
            }
        });

    }

    @Override
    public void displayCompanies(List<CompanySmall> companies, int currentPage, boolean isLastPage) {
        // prevent weird scroll to new item
        Parcelable recyclerViewState = companiesListView.getLayoutManager().onSaveInstanceState();
        adapter.setData(companies, currentPage, isLastPage);
        companiesListView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
    }


    public void displayError(Throwable error) {
        if (error instanceof ConnectionException) {
            super.displayError(getString(R.string.no_connection_error));
        } else if (error instanceof ProviderException) {
            super.displayError(getString(R.string.internal_error, error.getMessage()));
        } else {
            error.printStackTrace();
            super.displayError(error);
        }
    }

    @Override
    public void displayCompanyDetailsView(CompanySmall company) {
        DetailView detailView = getMainDisplay().displayView(DetailView.class);
        detailView.setCompany(company);
    }

    @Override
    public void setLoadingNextPageFailed() {
        adapter.setLoadAgain();
    }
}
