package com.tkolbusz.data.repository;

import com.tkolbusz.domain.exception.ConnectionException;
import com.tkolbusz.domain.exception.ProviderException;
import com.tkolbusz.domain.model.Company;
import com.tkolbusz.domain.model.CompanyLayer;
import com.tkolbusz.domain.model.CompanySmall;
import com.tkolbusz.provider.IProviderService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CompanyRepositoryTest {
    @Mock
    IProviderService providerService;

    @Test
    public void companyListIsDownloaded() throws ConnectionException, ProviderException {
        CompanyRepositoryImpl repository = getInstance();
        CompanySmall small = new CompanySmall(1, "test", "test", null, "123", new Date());
        List<CompanySmall> list = Arrays.asList(small);
        when(providerService.searchCompanies(anyString(), anyInt(), anyInt())).thenReturn(list);
        List<CompanySmall> returnVal = repository.getCompanies("test", 1);
        assertEquals(list, returnVal);

        verify(providerService).searchCompanies(eq("test"), eq(1), anyInt());
        verifyNoMoreInteractions(providerService);
    }

    @Test
    public void gettingCompanyByIdIsCached() throws ConnectionException, ProviderException {
        CompanyRepositoryImpl repository = getInstance();
        Company company = new Company(1, "test", "test", null, "123", null, null, null, null, null, null);
        when(providerService.getCompanyById(anyInt(), ArgumentMatchers.<CompanyLayer>anyList())).thenReturn(company);
        repository.getCompanyById(1);
        repository.getCompanyById(1);

        verify(providerService, times(1)).getCompanyById(anyInt(), ArgumentMatchers.<CompanyLayer>anyList());

        verifyNoMoreInteractions(providerService);
    }

    private CompanyRepositoryImpl getInstance() {
        return new CompanyRepositoryImpl(providerService);
    }

}