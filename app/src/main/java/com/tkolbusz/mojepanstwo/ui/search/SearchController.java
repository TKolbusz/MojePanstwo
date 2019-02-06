package com.tkolbusz.mojepanstwo.ui.search;

import com.tkolbusz.domain.command.companies.SearchCompanies;
import com.tkolbusz.domain.model.CompanySmall;
import com.tkolbusz.domain.model.PaginationResult;
import com.tkolbusz.domain.model.QueryData;
import com.tkolbusz.domain.threading.IPostExecutionThread;
import com.tkolbusz.domain.view.SearchContract;
import com.tkolbusz.mojepanstwo.base.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;

public class SearchController extends Controller<SearchContract.View> implements SearchContract.Controller {
    private final SearchCompanies searchCompaniesCommand;
    private final IPostExecutionThread postExecutionThread;

    private final PublishSubject<QueryData> queryPublisher;
    private final PublishSubject<QueryData> nextPagePublisher;

    @Inject
    public SearchController(SearchCompanies searchCompaniesCommand, IPostExecutionThread postExecutionThread) {
        this.searchCompaniesCommand = searchCompaniesCommand;
        this.postExecutionThread = postExecutionThread;
        this.queryPublisher = PublishSubject.create();
        this.nextPagePublisher = PublishSubject.create();
    }

    @Override
    protected void onViewInjected() {
        registerDisposable(createGetCompaniesStream());
    }

    @Override
    public void onNewSearchQuery(QueryData query) {
        queryPublisher.onNext(query);
    }

    @Override
    public void onLoadMoreData(QueryData queryData) {
        nextPagePublisher.onNext(queryData);
    }

    private Disposable createGetCompaniesStream() {
        return queryPublisher.debounce(200, TimeUnit.MILLISECONDS, postExecutionThread.getScheduler())
                .mergeWith(nextPagePublisher)
                .switchMap(queryData -> getCompanies(queryData))
                .scan((previousResult, nextResult) -> {
                    if (nextResult.getNextPage() == 1 || nextResult.getNextPage() == 0)
                        return nextResult;
                    List<CompanySmall> mergedList = new ArrayList<>(previousResult.getData());
                    mergedList.addAll(nextResult.getData());
                    return PaginationResult.from(mergedList, nextResult.getNextPage(), nextResult.isLastPage());
                })
                .doOnNext(result -> getView().displayCompanies(result.getData(), result.getNextPage(), result.isLastPage()))
                .subscribe();
    }

    private Observable<PaginationResult<CompanySmall>> getCompanies(QueryData queryData) {
        return searchCompaniesCommand.apply(new SearchCompanies.Params(queryData.getQuery(), queryData.getPageToLoad()))
                .onErrorResumeNext(error -> {
                    getView().displayError(error);
                    getView().setLoadingNextPageFailed();
                    return Observable.empty();
                });
    }

    @Override
    public void onCompanySelected(CompanySmall companySmall) {
        getView().displayCompanyDetailsView(companySmall);
    }

}