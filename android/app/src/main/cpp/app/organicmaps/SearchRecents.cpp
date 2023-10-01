// This file is modified by Zheng-Xiang Ke on 2022.
#include "Framework.hpp"

#include "search/result.hpp"

#include "app/organicmaps/core/jni_helper.hpp"
#include "app/organicmaps/core/jni_java_methods.hpp"

using SearchRequest = search::QuerySaver::SearchRequest;

extern "C"
{
  JNIEXPORT void JNICALL
  Java_app_organicmaps_search_SearchRecents_nativeGetList(JNIEnv * env, jclass thiz, jobject result)
  {
    auto const & items = g_framework->NativeFramework()->GetSearchAPI().GetLastSearchQueries();
    if (items.empty())
      return;

    auto const & pairBuilder = jni::PairBuilder::Instance(env);
    auto const listAddMethod = jni::ListBuilder::Instance(env).m_add;

    for (SearchRequest const & item : items)
    {
      using SLR = jni::TScopedLocalRef;
      SLR pair(env, pairBuilder.Create(env, SLR(env, jni::ToJavaString(env, item.first)),
                                            SLR(env, jni::ToJavaString(env, item.second))));
      ASSERT(pair.get(), (jni::DescribeException()));

      env->CallBooleanMethod(result, listAddMethod, pair.get());
    }
  }

  JNIEXPORT void JNICALL
  Java_app_organicmaps_search_SearchRecents_nativeAdd(JNIEnv * env, jclass thiz, jstring locale, jstring query)
  {
    SearchRequest const sr(jni::ToNativeString(env, locale), jni::ToNativeString(env, query));
    g_framework->NativeFramework()->GetSearchAPI().SaveSearchQuery(sr);
  }

  JNIEXPORT void JNICALL
  Java_app_organicmaps_search_SearchRecents_nativeRemove(JNIEnv * env, jclass thiz, jstring query)
  {
    auto & searchAPI = g_framework->NativeFramework()->GetSearchAPI();
    auto const & items = searchAPI.GetLastSearchQueries();
    if (items.empty())
      return;

    auto queryString = jni::ToNativeString(env, query);
    auto nonConstItems = items;
    nonConstItems.remove_if([&](search::QuerySaver::SearchRequest r) { return r.second == queryString; });
    searchAPI.ClearSearchHistory();
    nonConstItems.reverse();
    for (auto item : nonConstItems) {
      searchAPI.SaveSearchQuery(item);
    }
  }

  JNIEXPORT void JNICALL
  Java_app_organicmaps_search_SearchRecents_nativeClear(JNIEnv * env, jclass thiz)
  {
    g_framework->NativeFramework()->GetSearchAPI().ClearSearchHistory();
  }
}
