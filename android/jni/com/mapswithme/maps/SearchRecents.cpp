// This file is modified by Zheng-Xiang Ke on 2022.
#include "Framework.hpp"

#include "search/result.hpp"

#include "com/mapswithme/core/jni_helper.hpp"

using SearchRequest = search::QuerySaver::SearchRequest;

extern "C"
{
  JNIEXPORT void JNICALL
  Java_com_mapswithme_maps_search_SearchRecents_nativeGetList(JNIEnv * env, jclass thiz, jobject result)
  {
    auto const & items = g_framework->NativeFramework()->GetSearchAPI().GetLastSearchQueries();
    if (items.empty())
      return;

    static jclass const pairClass = jni::GetGlobalClassRef(env, "android/util/Pair");
    static jmethodID const pairCtor = jni::GetConstructorID(env, pairClass, "(Ljava/lang/Object;Ljava/lang/Object;)V");
    static jmethodID const listAddMethod = jni::GetMethodID(env, result, "add", "(Ljava/lang/Object;)Z");

    for (SearchRequest const & item : items)
    {
      jni::TScopedLocalRef locale(env, jni::ToJavaString(env, item.first.c_str()));
      jni::TScopedLocalRef query(env, jni::ToJavaString(env, item.second.c_str()));
      jni::TScopedLocalRef pair(env, env->NewObject(pairClass, pairCtor, locale.get(), query.get()));
      ASSERT(pair.get(), (jni::DescribeException()));

      env->CallBooleanMethod(result, listAddMethod, pair.get());
    }
  }

  JNIEXPORT void JNICALL
  Java_com_mapswithme_maps_search_SearchRecents_nativeAdd(JNIEnv * env, jclass thiz, jstring locale, jstring query)
  {
    SearchRequest const sr(jni::ToNativeString(env, locale), jni::ToNativeString(env, query));
    g_framework->NativeFramework()->GetSearchAPI().SaveSearchQuery(sr);
  }

  JNIEXPORT void JNICALL
  Java_com_mapswithme_maps_search_SearchRecents_nativeRemove(JNIEnv * env, jclass thiz, jstring query)
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
  Java_com_mapswithme_maps_search_SearchRecents_nativeClear(JNIEnv * env, jclass thiz)
  {
    g_framework->NativeFramework()->GetSearchAPI().ClearSearchHistory();
  }
}
