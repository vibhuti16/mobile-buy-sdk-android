/*
 *   The MIT License (MIT)
 *
 *   Copyright (c) 2015 Shopify Inc.
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 */

package com.shopify.sample.interactor.cart;

import android.support.annotation.NonNull;

import com.shopify.buy3.GraphCall;
import com.shopify.buy3.GraphClient;
import com.shopify.buy3.Storefront;
import com.shopify.graphql.support.ID;
import com.shopify.sample.SampleApplication;
import com.shopify.sample.presenter.cart.Checkout;

import java.util.List;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

import static com.shopify.sample.RxUtil.rxGraphMutationCall;
import static com.shopify.sample.util.Util.checkNotEmpty;
import static com.shopify.sample.util.Util.mapItems;

public final class RealCreateCheckout implements CreateCheckout {
  private final GraphClient graphClient = SampleApplication.graphClient();

  @Override public Single<Checkout> call(@NonNull final List<LineItem> lineItems) {
    checkNotEmpty(lineItems, "lineItems can't be empty");

    List<Storefront.LineItemInput> storefrontLineItems = mapItems(lineItems, lineItem ->
      new Storefront.LineItemInput(lineItem.quantity, new ID(lineItem.variantId)));
    Storefront.CheckoutCreateInput input = new Storefront.CheckoutCreateInput().setLineItems(storefrontLineItems);
    GraphCall<Storefront.Mutation> call = graphClient.mutateGraph(Storefront.mutation(
      root -> root.checkoutCreate(
        input,
        query -> query.checkout(
          checkout -> checkout
            .webUrl()
            .requiresShipping()
        )
      ))
    );
    return rxGraphMutationCall(call)
      .map(it -> it.data().getCheckoutCreate().getCheckout())
      .map(it -> new Checkout(it.getId().toString(), it.getWebUrl(), it.getRequiresShipping()))
      .subscribeOn(Schedulers.io());
  }
}