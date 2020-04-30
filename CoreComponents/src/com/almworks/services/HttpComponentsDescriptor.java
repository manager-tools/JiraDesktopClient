package com.almworks.services;

import com.almworks.api.container.RootContainer;
import com.almworks.api.http.HttpClientProvider;
import com.almworks.api.http.HttpLoaderFactory;
import com.almworks.api.http.HttpProxyInfo;
import com.almworks.api.http.auth.HttpAuthDialog;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.http.HttpClientProviderImpl;
import com.almworks.http.HttpLoaderFactoryImpl;
import com.almworks.http.HttpMaterialFactory;
import com.almworks.http.ui.HttpAuthDialogImpl;
import com.almworks.http.ui.HttpProxyInfoImpl;
import com.almworks.util.properties.Role;

public class HttpComponentsDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer container) {
    container.registerActorClass(HttpAuthDialog.ROLE, HttpAuthDialogImpl.class);
    container.registerActorClass(HttpProxyInfo.ROLE, HttpProxyInfoImpl.class);
    container.registerActorClass(HttpProxyAction.ROLE, HttpProxyAction.class);
    container.registerActorClass(HttpClientProvider.ROLE, HttpClientProviderImpl.class);
    container.registerActor(Role.anonymous(), HttpLoaderFactory.SingOnProvider.DUMMY); // To ensure at least one provider is registered
    container.registerActorClass(HttpLoaderFactory.ROLE, HttpLoaderFactoryImpl.class);
    container.registerActorClass(HttpMaterialFactory.ROLE, HttpMaterialFactory.class);
  }
}
