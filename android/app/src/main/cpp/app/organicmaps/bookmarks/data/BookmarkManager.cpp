// This file is modified by Zheng-Xiang Ke on 2023.
#include "app/organicmaps/core/jni_helper.hpp"
#include "app/organicmaps/Framework.hpp"
#include "app/organicmaps/UserMarkHelper.hpp"
#include "app/organicmaps/util/Distance.hpp"

#include "map/bookmark_helpers.hpp"
#include "map/place_page_info.hpp"

#include "geometry/circle_on_earth.hpp"

#include "coding/zip_creator.hpp"

#include "platform/localization.hpp"
#include "platform/preferred_languages.hpp"

#include "base/macros.hpp"
#include "base/string_utils.hpp"

#include <limits>
#include <utility>

#ifndef MIN
#define MIN(a,b) (((a)<(b)) ? (a) : (b))
#endif

#ifndef MAX
#define MAX(a,b) (((a)<(b)) ? (b) : (a))
#endif

using namespace jni;
using namespace std::placeholders;

namespace
{
::dp::Color initDpColor(int color) {
  auto const predefinedColor = kml::PredefinedColor(color);
  dp::Color dpcolor = dp::Color(0, 0, 0, 0);
  switch (predefinedColor)
  {
    case kml::PredefinedColor::Red: dpcolor = dp::Color(229, 27, 35, 204);
          break;
    case kml::PredefinedColor::Pink: dpcolor = dp::Color(255, 65, 130, 204);
          break;
    case kml::PredefinedColor::Purple: dpcolor = dp::Color(155, 36, 178, 204);
          break;
    case kml::PredefinedColor::DeepPurple: dpcolor = dp::Color(102, 57, 191, 204);
          break;
    case kml::PredefinedColor::Blue: dpcolor = dp::Color(0, 102, 204, 204);
          break;
    case kml::PredefinedColor::LightBlue: dpcolor = dp::Color(36, 156, 242, 204);
          break;
    case kml::PredefinedColor::Cyan: dpcolor = dp::Color(20, 190, 205, 204);
          break;
    case kml::PredefinedColor::Teal: dpcolor = dp::Color(0, 165, 140, 204);
          break;
    case kml::PredefinedColor::Green: dpcolor = dp::Color(60, 140, 60, 204);
          break;
    case kml::PredefinedColor::Lime: dpcolor = dp::Color(147, 191, 57, 204);
          break;
    case kml::PredefinedColor::Yellow: dpcolor = dp::Color(255, 200, 0, 204);
          break;
    case kml::PredefinedColor::Orange: dpcolor = dp::Color(255, 150, 0, 204);
          break;
    case kml::PredefinedColor::DeepOrange: dpcolor = dp::Color(240, 100, 50, 204);
          break;
    case kml::PredefinedColor::Brown: dpcolor = dp::Color(128, 70, 51, 204);
          break;
    case kml::PredefinedColor::Gray: dpcolor = dp::Color(115, 115, 115, 204);
          break;
    case kml::PredefinedColor::BlueGray: dpcolor = dp::Color(89, 115, 128, 204);
          break;
    case kml::PredefinedColor::None: dpcolor = dp::Color(0, 0, 0, 204);
          break;
    case kml::PredefinedColor::Count: dpcolor = dp::Color(0, 0, 0, 204);
          break;
  }
  return dpcolor;
}

jclass g_bookmarkManagerClass;
jfieldID g_bookmarkManagerInstanceField;
jmethodID g_onBookmarksChangedMethod;
jmethodID g_onBookmarksLoadingStartedMethod;
jmethodID g_onBookmarksLoadingFinishedMethod;
jmethodID g_onBookmarksFileLoadedMethod;
jmethodID g_onPreparedFileForSharingMethod;
jmethodID g_onElevationActivePointChangedMethod;
jmethodID g_onElevationCurrentPositionChangedMethod;
jclass g_bookmarkCategoryClass;
jmethodID g_bookmarkCategoryConstructor;

jclass g_sortedBlockClass;
jmethodID g_sortedBlockConstructor;
jclass g_longClass;
jmethodID g_longConstructor;
jmethodID g_onBookmarksSortingCompleted;
jmethodID g_onBookmarksSortingCancelled;
jmethodID g_bookmarkInfoConstructor;
jclass g_bookmarkInfoClass;

/**
 * Add by RobinChien 2020/07/10
 * */
int lineId = 0;

void PrepareClassRefs(JNIEnv * env)
{
  if (g_bookmarkManagerClass)
    return;

  g_bookmarkManagerClass =
    jni::GetGlobalClassRef(env, "app/organicmaps/bookmarks/data/BookmarkManager");
  g_bookmarkManagerInstanceField = jni::GetStaticFieldID(env, g_bookmarkManagerClass, "INSTANCE",
    "Lapp/organicmaps/bookmarks/data/BookmarkManager;");

  jobject bookmarkManagerInstance = env->GetStaticObjectField(g_bookmarkManagerClass,
                                                              g_bookmarkManagerInstanceField);
  g_onBookmarksChangedMethod =
    jni::GetMethodID(env, bookmarkManagerInstance, "onBookmarksChanged", "()V");
  g_onBookmarksLoadingStartedMethod =
    jni::GetMethodID(env, bookmarkManagerInstance, "onBookmarksLoadingStarted", "()V");
  g_onBookmarksLoadingFinishedMethod =
    jni::GetMethodID(env, bookmarkManagerInstance, "onBookmarksLoadingFinished", "()V");
  g_onBookmarksFileLoadedMethod =
    jni::GetMethodID(env, bookmarkManagerInstance, "onBookmarksFileLoaded",
                     "(ZLjava/lang/String;Z)V");
  g_onPreparedFileForSharingMethod =
    jni::GetMethodID(env, bookmarkManagerInstance, "onPreparedFileForSharing",
                     "(Lapp/organicmaps/bookmarks/data/BookmarkSharingResult;)V");

  g_longClass = jni::GetGlobalClassRef(env,"java/lang/Long");
  g_longConstructor = jni::GetConstructorID(env, g_longClass, "(J)V");
  g_sortedBlockClass =
    jni::GetGlobalClassRef(env, "app/organicmaps/bookmarks/data/SortedBlock");
  g_sortedBlockConstructor =
    jni::GetConstructorID(env, g_sortedBlockClass,
                          "(Ljava/lang/String;[Ljava/lang/Long;[Ljava/lang/Long;)V");


  g_onBookmarksSortingCompleted = jni::GetMethodID(env, bookmarkManagerInstance,
    "onBookmarksSortingCompleted", "([Lapp/organicmaps/bookmarks/data/SortedBlock;J)V");
  g_onBookmarksSortingCancelled = jni::GetMethodID(env, bookmarkManagerInstance,
    "onBookmarksSortingCancelled", "(J)V");
  g_bookmarkInfoClass =
    jni::GetGlobalClassRef(env, "app/organicmaps/bookmarks/data/BookmarkInfo");
  g_bookmarkInfoConstructor =
    jni::GetConstructorID(env, g_bookmarkInfoClass, "(JJ)V" );
  g_bookmarkCategoryClass =
    jni::GetGlobalClassRef(env, "app/organicmaps/bookmarks/data/BookmarkCategory");

//public BookmarkCategory(long id,
//                          String name,
//                          String annotation,
//                          String desc,
//                          int tracksCount,
//                          int bookmarksCount,
//                          boolean isVisible)
  g_bookmarkCategoryConstructor =
      jni::GetConstructorID(env, g_bookmarkCategoryClass,
                            "("
                            "J"                   // id
                            "Ljava/lang/String;"  // name
                            "Ljava/lang/String;"  // annotation
                            "Ljava/lang/String;"  // desc
                            "I"                   // tracksCount
                            "I"                   // bookmarksCount
                            "Z"                   // isVisible
                            ")V");
  g_onElevationCurrentPositionChangedMethod =
      jni::GetMethodID(env, bookmarkManagerInstance, "onElevationCurrentPositionChanged", "()V");
  g_onElevationActivePointChangedMethod =
      jni::GetMethodID(env, bookmarkManagerInstance, "onElevationActivePointChanged", "()V");
}

void OnElevationCurPositionChanged(JNIEnv * env)
{
  ASSERT(g_bookmarkManagerClass, ());
  jobject bookmarkManagerInstance =
      env->GetStaticObjectField(g_bookmarkManagerClass, g_bookmarkManagerInstanceField);
  env->CallVoidMethod(bookmarkManagerInstance, g_onElevationCurrentPositionChangedMethod);
  jni::HandleJavaException(env);
}

void OnElevationActivePointChanged(JNIEnv * env)
{
  ASSERT(g_bookmarkManagerClass, ());
  jobject bookmarkManagerInstance = env->GetStaticObjectField(g_bookmarkManagerClass,
                                                              g_bookmarkManagerInstanceField);
  env->CallVoidMethod(bookmarkManagerInstance, g_onElevationActivePointChangedMethod);
  jni::HandleJavaException(env);
}

void OnBookmarksChanged(JNIEnv * env)
{
  ASSERT(g_bookmarkManagerClass, ());
  jobject bookmarkManagerInstance = env->GetStaticObjectField(g_bookmarkManagerClass,
                                                              g_bookmarkManagerInstanceField);
  env->CallVoidMethod(bookmarkManagerInstance, g_onBookmarksChangedMethod);
  jni::HandleJavaException(env);
}

void OnAsyncLoadingStarted(JNIEnv * env)
{
  ASSERT(g_bookmarkManagerClass, ());
  jobject bookmarkManagerInstance = env->GetStaticObjectField(g_bookmarkManagerClass,
                                                              g_bookmarkManagerInstanceField);
  env->CallVoidMethod(bookmarkManagerInstance, g_onBookmarksLoadingStartedMethod);
  jni::HandleJavaException(env);
}

void OnAsyncLoadingFinished(JNIEnv * env)
{
  ASSERT(g_bookmarkManagerClass, ());
  jobject bookmarkManagerInstance = env->GetStaticObjectField(g_bookmarkManagerClass,
                                                              g_bookmarkManagerInstanceField);
  env->CallVoidMethod(bookmarkManagerInstance, g_onBookmarksLoadingFinishedMethod);
  jni::HandleJavaException(env);
}

void OnAsyncLoadingFileSuccess(JNIEnv * env, std::string const & fileName, bool isTemporaryFile)
{
  ASSERT(g_bookmarkManagerClass, ());
  jobject bookmarkManagerInstance = env->GetStaticObjectField(g_bookmarkManagerClass,
                                                              g_bookmarkManagerInstanceField);
  jni::TScopedLocalRef jFileName(env, jni::ToJavaString(env, fileName));
  env->CallVoidMethod(bookmarkManagerInstance, g_onBookmarksFileLoadedMethod,
                      true /* success */, jFileName.get(), isTemporaryFile);
  jni::HandleJavaException(env);
}

void OnAsyncLoadingFileError(JNIEnv * env, std::string const & fileName, bool isTemporaryFile)
{
  ASSERT(g_bookmarkManagerClass, ());
  jobject bookmarkManagerInstance = env->GetStaticObjectField(g_bookmarkManagerClass,
                                                              g_bookmarkManagerInstanceField);
  jni::TScopedLocalRef jFileName(env, jni::ToJavaString(env, fileName));
  env->CallVoidMethod(bookmarkManagerInstance, g_onBookmarksFileLoadedMethod,
                      false /* success */, jFileName.get(), isTemporaryFile);
  jni::HandleJavaException(env);
}

void OnPreparedFileForSharing(JNIEnv * env, BookmarkManager::SharingResult const & result)
{
  static jclass const classBookmarkSharingResult = jni::GetGlobalClassRef(env, "app/organicmaps/bookmarks/data/BookmarkSharingResult");
  // BookmarkSharingResult(long[] categoriesIds, @Code int code, @NonNull String sharingPath, @NonNull String mimeType, @NonNull String errorString)
  static jmethodID const ctorBookmarkSharingResult = jni::GetConstructorID(env, classBookmarkSharingResult, "([JILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

  static_assert(sizeof(jlong) == sizeof(decltype(result.m_categoriesIds)::value_type));
  jsize const categoriesIdsSize = static_cast<jsize>(result.m_categoriesIds.size());
  jni::ScopedLocalRef<jlongArray> categoriesIds(env, env->NewLongArray(categoriesIdsSize));
  env->SetLongArrayRegion(categoriesIds.get(), 0, categoriesIdsSize, reinterpret_cast<jlong const *>(result.m_categoriesIds.data()));
  jni::TScopedLocalRef const sharingPath(env, jni::ToJavaString(env, result.m_sharingPath));
  jni::TScopedLocalRef const mimeType(env, jni::ToJavaString(env, result.m_mimeType));
  jni::TScopedLocalRef const errorString(env, jni::ToJavaString(env, result.m_errorString));

  jni::TScopedLocalRef const sharingResult(env, env->NewObject(classBookmarkSharingResult, ctorBookmarkSharingResult,
      categoriesIds.get(), static_cast<jint>(result.m_code), sharingPath.get(), mimeType.get(), errorString.get()));

  ASSERT(g_bookmarkManagerClass, ());
  jobject bookmarkManagerInstance = env->GetStaticObjectField(g_bookmarkManagerClass, g_bookmarkManagerInstanceField);
  env->CallVoidMethod(bookmarkManagerInstance, g_onPreparedFileForSharingMethod, sharingResult.get());
  jni::HandleJavaException(env);
}

void OnCategorySortingResults(JNIEnv * env, long long timestamp,
                              BookmarkManager::SortedBlocksCollection && sortedBlocks,
                              BookmarkManager::SortParams::Status status)
{
  ASSERT(g_bookmarkManagerClass, ());
  ASSERT(g_sortedBlockClass, ());
  ASSERT(g_sortedBlockConstructor, ());

  jobject bookmarkManagerInstance = env->GetStaticObjectField(g_bookmarkManagerClass,
                                                              g_bookmarkManagerInstanceField);

  if (status == BookmarkManager::SortParams::Status::Cancelled)
  {
    env->CallVoidMethod(bookmarkManagerInstance, g_onBookmarksSortingCancelled,
                        static_cast<jlong>(timestamp));
    jni::HandleJavaException(env);
    return;
  }

  jni::TScopedLocalObjectArrayRef blocksRef(env,
      jni::ToJavaArray(env, g_sortedBlockClass, sortedBlocks,
          [](JNIEnv * env, BookmarkManager::SortedBlock const & block)
          {
            jni::TScopedLocalRef blockNameRef(env, jni::ToJavaString(env, block.m_blockName));

            jni::TScopedLocalObjectArrayRef marksRef(env,
                jni::ToJavaArray(env, g_longClass, block.m_markIds,
                    [](JNIEnv * env, kml::MarkId const & markId)
                    {
                      return env->NewObject(g_longClass, g_longConstructor,
                          static_cast<jlong>(markId));
                    }));

            jni::TScopedLocalObjectArrayRef tracksRef(env,
                jni::ToJavaArray(env, g_longClass, block.m_trackIds,
                    [](JNIEnv * env, kml::TrackId const & trackId)
                    {
                      return env->NewObject(g_longClass, g_longConstructor,
                          static_cast<jlong>(trackId));
                    }));

            return env->NewObject(g_sortedBlockClass, g_sortedBlockConstructor,
                                 blockNameRef.get(), marksRef.get(), tracksRef.get());

          }));
  env->CallVoidMethod(bookmarkManagerInstance, g_onBookmarksSortingCompleted,
                      blocksRef.get(), static_cast<jlong>(timestamp));
  jni::HandleJavaException(env);
}

Bookmark const * getBookmark(jlong bokmarkId)
{
  Bookmark const * pBmk = frm()->GetBookmarkManager().GetBookmark(static_cast<kml::MarkId>(bokmarkId));
//  ASSERT(pBmk, ("Bookmark not found, id", bokmarkId));
  return pBmk;
}

jobject MakeCategory(JNIEnv * env, kml::MarkGroupId id)
{
  auto const & manager = frm()->GetBookmarkManager();
  auto const & data = manager.GetCategoryData(id);

  auto const tracksCount = manager.GetTrackIds(data.m_id).size();
  auto const bookmarksCount = manager.GetUserMarkIds(data.m_id).size();
  auto const isVisible = manager.IsVisible(data.m_id);
  auto const preferBookmarkStr = GetPreferredBookmarkStr(data.m_name);
  auto const annotation = GetPreferredBookmarkStr(data.m_annotation);
  auto const description = GetPreferredBookmarkStr(data.m_description);

  jni::TScopedLocalRef preferBookmarkStrRef(env, jni::ToJavaString(env, preferBookmarkStr));
  jni::TScopedLocalRef annotationRef(env, jni::ToJavaString(env, annotation));
  jni::TScopedLocalRef descriptionRef(env, jni::ToJavaString(env, description));

  return env->NewObject(g_bookmarkCategoryClass,
                        g_bookmarkCategoryConstructor,
                        static_cast<jlong>(data.m_id),
                        preferBookmarkStrRef.get(),
                        annotationRef.get(),
                        descriptionRef.get(),
                        static_cast<jint>(tracksCount),
                        static_cast<jint>(bookmarksCount),
                        static_cast<jboolean>(isVisible));
}

jobjectArray MakeCategories(JNIEnv * env, kml::GroupIdCollection const & ids)
{
  return ToJavaArray(env, g_bookmarkCategoryClass, ids, std::bind(&MakeCategory, _1, _2));
}
}  // namespace

extern "C"
{
JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeShowBookmarkOnMap(
    JNIEnv *, jobject, jlong bmkId)
{
  frm()->ShowBookmark(static_cast<kml::MarkId>(bmkId));
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeShowBookmarkCategoryOnMap(
    JNIEnv *, jobject, jlong catId)
{
  frm()->ShowBookmarkCategory(static_cast<kml::MarkGroupId>(catId), true /* animated */);
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeLoadBookmarks(JNIEnv * env, jclass)
{
  PrepareClassRefs(env);
  BookmarkManager::AsyncLoadingCallbacks callbacks;
  callbacks.m_onStarted = std::bind(&OnAsyncLoadingStarted, env);
  callbacks.m_onFinished = std::bind(&OnAsyncLoadingFinished, env);
  callbacks.m_onFileSuccess = std::bind(&OnAsyncLoadingFileSuccess, env, _1, _2);
  callbacks.m_onFileError = std::bind(&OnAsyncLoadingFileError, env, _1, _2);
  frm()->GetBookmarkManager().SetAsyncLoadingCallbacks(std::move(callbacks));

  frm()->GetBookmarkManager().SetBookmarksChangedCallback(std::bind(&OnBookmarksChanged, env));

  frm()->LoadBookmarks();
}

JNIEXPORT jlong JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeCreateCategory(
     JNIEnv * env, jobject, jstring name)
{
  auto const categoryId = frm()->GetBookmarkManager().CreateBookmarkCategory(ToNativeString(env, name));
  frm()->GetBookmarkManager().SetLastEditedBmCategory(categoryId);
  return static_cast<jlong>(categoryId);
}

JNIEXPORT jboolean JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeDeleteCategory(
     JNIEnv *, jobject, jlong catId)
{
  auto const categoryId = static_cast<kml::MarkGroupId>(catId);
  // `permanently` should be set to false when the Recently Deleted Lists feature be implemented
  return static_cast<jboolean>(frm()->GetBookmarkManager().GetEditSession().DeleteBmCategory(categoryId, true /* permanently */));
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeDeleteBookmark(JNIEnv *, jobject, jlong bmkId)
{
  frm()->GetBookmarkManager().GetEditSession().DeleteBookmark(static_cast<kml::MarkId>(bmkId));
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeDeleteTrack(
    JNIEnv *, jobject, jlong trkId)
{
  frm()->GetBookmarkManager().GetEditSession().DeleteTrack(static_cast<kml::TrackId>(trkId));
}

JNIEXPORT jobject JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeAddBookmarkToLastEditedCategory(
    JNIEnv * env, jobject, double lat, double lon)
{
  if (!frm()->HasPlacePageInfo())
    return nullptr;

  BookmarkManager & bmMng = frm()->GetBookmarkManager();

  place_page::Info const & info = g_framework->GetPlacePageInfo();

  kml::BookmarkData bmData;
  bmData.m_name = info.FormatNewBookmarkName();
  bmData.m_color.m_predefinedColor = frm()->LastEditedBMColor();
  bmData.m_point = mercator::FromLatLon(lat, lon);
  auto const lastEditedCategory = frm()->LastEditedBMCategory();

  if (info.IsFeature())
    SaveFeatureTypes(info.GetTypes(), bmData);

  auto const * createdBookmark = bmMng.GetEditSession().CreateBookmark(std::move(bmData),
    lastEditedCategory);

  auto buildInfo = info.GetBuildInfo();
  buildInfo.m_match = place_page::BuildInfo::Match::Everything;
  buildInfo.m_userMarkId = createdBookmark->GetId();
  frm()->UpdatePlacePageInfoForCurrentSelection(buildInfo);

  return usermark_helper::CreateMapObject(env, g_framework->GetPlacePageInfo());
}

JNIEXPORT jlong JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetLastEditedCategory(
      JNIEnv *, jobject)
{
  return static_cast<jlong>(frm()->LastEditedBMCategory());
}

JNIEXPORT jint JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetLastEditedColor(
        JNIEnv *, jobject)
{
  return static_cast<jint>(frm()->LastEditedBMColor());
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeLoadBookmarksFile(JNIEnv * env, jclass,
                                                                                jstring path, jboolean isTemporaryFile)
{
  frm()->AddBookmarksFile(ToNativeString(env, path), isTemporaryFile);
}

JNIEXPORT jboolean JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeIsAsyncBookmarksLoadingInProgress(JNIEnv *, jclass)
{
  return static_cast<jboolean>(frm()->GetBookmarkManager().IsAsyncLoadingInProgress());
}

JNIEXPORT jboolean JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeIsVisible(
    JNIEnv *, jobject, jlong catId)
{
  return static_cast<jboolean>(frm()->GetBookmarkManager().IsVisible(static_cast<kml::MarkGroupId>(catId)));
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeSetVisibility(
    JNIEnv *, jobject, jlong catId, jboolean isVisible)
{
  frm()->GetBookmarkManager().GetEditSession().SetIsVisible(static_cast<kml::MarkGroupId>(catId), isVisible);
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeSetCategoryName(
    JNIEnv * env, jobject, jlong catId, jstring name)
{
  frm()->GetBookmarkManager().GetEditSession().SetCategoryName(static_cast<kml::MarkGroupId>(catId),
                                                               jni::ToNativeString(env, name));
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeSetCategoryDescription(JNIEnv * env,
                                                                                     jobject,
                                                                                     jlong catId,
                                                                                     jstring desc)
{
  frm()->GetBookmarkManager().GetEditSession().SetCategoryDescription(
      static_cast<kml::MarkGroupId>(catId), jni::ToNativeString(env, desc));
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeSetCategoryTags(
    JNIEnv * env, jobject, jlong catId, jobjectArray tagsIds)
{
  auto const size = env->GetArrayLength(tagsIds);
  std::vector<std::string> categoryTags;
  categoryTags.reserve(static_cast<size_t>(size));
  for (auto i = 0; i < size; i++)
  {
    jni::TScopedLocalRef const item(env, env->GetObjectArrayElement(tagsIds, i));
    categoryTags.push_back(jni::ToNativeString(env, static_cast<jstring>(item.get())));
  }

  frm()->GetBookmarkManager().GetEditSession().SetCategoryTags(static_cast<kml::MarkGroupId>(catId),
                                                               categoryTags);
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeSetCategoryAccessRules(
    JNIEnv *, jobject, jlong catId, jint accessRules)
{
  frm()->GetBookmarkManager().GetEditSession().SetCategoryAccessRules(
    static_cast<kml::MarkGroupId>(catId), static_cast<kml::AccessRules>(accessRules));
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeSetCategoryCustomProperty(
    JNIEnv * env, jobject, jlong catId, jstring key, jstring value)
{
  frm()->GetBookmarkManager().GetEditSession().SetCategoryCustomProperty(
    static_cast<kml::MarkGroupId>(catId), ToNativeString(env, key), ToNativeString(env, value));
}

JNIEXPORT jobject JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeUpdateBookmarkPlacePage(
     JNIEnv * env, jobject, jlong bmkId)
{
  if (!frm()->HasPlacePageInfo())
    return nullptr;

  auto & info = g_framework->GetPlacePageInfo();
  auto buildInfo = info.GetBuildInfo();
  buildInfo.m_userMarkId = static_cast<kml::MarkId>(bmkId);
  frm()->UpdatePlacePageInfoForCurrentSelection(buildInfo);

  return usermark_helper::CreateMapObject(env, g_framework->GetPlacePageInfo());
}

JNIEXPORT jobject JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetBookmarkInfo(
  JNIEnv * env, jobject, jlong bmkId)
{
  auto const bookmark = frm()->GetBookmarkManager().GetBookmark(static_cast<kml::MarkId>(bmkId));
  if (!bookmark)
    return nullptr;
  return env->NewObject(g_bookmarkInfoClass,
                        g_bookmarkInfoConstructor, static_cast<jlong>(bookmark->GetGroupId()),
                        static_cast<jlong>(bmkId));
}

JNIEXPORT jlong JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetBookmarkIdByPosition(
        JNIEnv *, jobject, jlong catId, jint positionInCategory)
{
  auto const & ids = frm()->GetBookmarkManager().GetUserMarkIds(static_cast<kml::MarkGroupId>(catId));
  if (positionInCategory >= static_cast<jlong>(ids.size()))
    return static_cast<jlong>(kml::kInvalidMarkId);
  auto it = ids.begin();
  std::advance(it, positionInCategory);
  return static_cast<jlong>(*it);
}

static uint32_t shift(uint32_t v, uint8_t bitCount) { return v << bitCount; }

JNIEXPORT jobject JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetTrack(
      JNIEnv * env, jobject, jlong trackId, jclass trackClazz)
{
  // Track(long trackId, long categoryId, String name, String lengthString, int color)
  static jmethodID const cId = jni::GetConstructorID(env, trackClazz,
                                                     "(JJLjava/lang/String;Lapp/organicmaps/util/Distance;I)V");
  auto const * nTrack = frm()->GetBookmarkManager().GetTrack(static_cast<kml::TrackId>(trackId));

  ASSERT(nTrack, ("Track must not be null with id:)", trackId));

  dp::Color nColor = nTrack->GetColor(0);

  jint androidColor = shift(nColor.GetAlpha(), 24) +
                      shift(nColor.GetRed(), 16) +
                      shift(nColor.GetGreen(), 8) +
                      nColor.GetBlue();

  return env->NewObject(trackClazz, cId,
                        trackId, static_cast<jlong>(nTrack->GetGroupId()), jni::ToJavaString(env, nTrack->GetName()),
                        ToJavaDistance(env, platform::Distance::CreateFormatted(nTrack->GetLengthMeters())), androidColor);
}

JNIEXPORT jlong JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetTrackIdByPosition(
        JNIEnv *, jobject, jlong catId, jint positionInCategory)
{
  auto const & ids = frm()->GetBookmarkManager().GetTrackIds(static_cast<kml::MarkGroupId>(catId));
  if (positionInCategory >= static_cast<jlong>(ids.size()))
    return static_cast<jlong>(kml::kInvalidTrackId);
  auto it = ids.begin();
  std::advance(it, positionInCategory);
  return static_cast<jlong>(*it);
}

JNIEXPORT jboolean JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeIsUsedCategoryName(
        JNIEnv * env, jclass, jstring name)
{
  return static_cast<jboolean>(frm()->GetBookmarkManager().IsUsedCategoryName(ToNativeString(env, name)));
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativePrepareForSearch(
        JNIEnv *, jclass, jlong catId)
{
  frm()->GetBookmarkManager().PrepareForSearch(static_cast<kml::MarkGroupId>(catId));
}

JNIEXPORT jboolean JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeAreAllCategoriesInvisible(
        JNIEnv *, jclass)
{
  return static_cast<jboolean>(frm()->GetBookmarkManager().AreAllCategoriesInvisible());
}

JNIEXPORT jboolean JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeAreAllCategoriesVisible(
        JNIEnv *, jclass)
{
  return static_cast<jboolean>(frm()->GetBookmarkManager().AreAllCategoriesVisible());
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeSetAllCategoriesVisibility(
        JNIEnv *, jclass, jboolean visible)
{
  frm()->GetBookmarkManager().SetAllCategoriesVisibility(static_cast<bool>(visible));
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativePrepareFileForSharing(JNIEnv * env, jclass, jlongArray catIds, jint kmlFileType)
{
  auto const size = env->GetArrayLength(catIds);
  kml::GroupIdCollection catIdsVector(size);
  static_assert(sizeof(jlong) == sizeof(decltype(catIdsVector)::value_type));
  env->GetLongArrayRegion(catIds, 0, size, reinterpret_cast<jlong *>(catIdsVector.data()));
  frm()->GetBookmarkManager().PrepareFileForSharing(std::move(catIdsVector), [env](BookmarkManager::SharingResult const & result)
  {
    OnPreparedFileForSharing(env, result);
  }, static_cast<KmlFileType>(kmlFileType));
}

JNIEXPORT jboolean JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeIsCategoryEmpty(
        JNIEnv *, jclass, jlong catId)
{
  return static_cast<jboolean>(frm()->GetBookmarkManager().IsCategoryEmpty(
    static_cast<kml::MarkGroupId>(catId)));
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeSetNotificationsEnabled(
        JNIEnv *, jclass, jboolean enabled)
{
  frm()->GetBookmarkManager().SetNotificationsEnabled(static_cast<bool>(enabled));
}

JNIEXPORT jboolean JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeAreNotificationsEnabled(
        JNIEnv *, jclass)
{
  return static_cast<jboolean>(frm()->GetBookmarkManager().AreNotificationsEnabled());
}

JNIEXPORT jobject JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetBookmarkCategory(JNIEnv *env, jobject, jlong id)
{
  return MakeCategory(env, static_cast<kml::MarkGroupId>(id));
}

JNIEXPORT jobjectArray JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetBookmarkCategories(JNIEnv *env, jobject)
{
  auto const & bm = frm()->GetBookmarkManager();
  auto const & ids = bm.GetSortedBmGroupIdList();

  return MakeCategories(env, ids);
}

JNIEXPORT jint JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetBookmarkCategoriesCount(JNIEnv *env, jobject)
{
   auto const & bm = frm()->GetBookmarkManager();
   auto const count = bm.GetBmGroupsCount();

   return static_cast<jint>(count);
}

JNIEXPORT jobjectArray JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetChildrenCategories(JNIEnv *env, jobject, jlong parentId)
{
  auto const & bm = frm()->GetBookmarkManager();
  auto const ids = bm.GetChildrenCategories(static_cast<kml::MarkGroupId>(parentId));

  return MakeCategories(env, ids);
}

JNIEXPORT jboolean JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeHasLastSortingType(
    JNIEnv *, jobject, jlong catId)
{
  auto const & bm = frm()->GetBookmarkManager();
  BookmarkManager::SortingType type;
  return static_cast<jboolean>(bm.GetLastSortingType(static_cast<kml::MarkGroupId>(catId), type));
}

JNIEXPORT jint JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetLastSortingType(
    JNIEnv *, jobject, jlong catId)
{
  auto const & bm = frm()->GetBookmarkManager();
  BookmarkManager::SortingType type;
  auto const hasType = bm.GetLastSortingType(static_cast<kml::MarkGroupId>(catId), type);
  ASSERT(hasType, ());
  UNUSED_VALUE(hasType);
  return static_cast<jint>(type);
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeSetLastSortingType(
    JNIEnv *, jobject, jlong catId, jint type)
{
  auto & bm = frm()->GetBookmarkManager();
  bm.SetLastSortingType(static_cast<kml::MarkGroupId>(catId),
      static_cast<BookmarkManager::SortingType>(type));
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeResetLastSortingType(
    JNIEnv *, jobject, jlong catId)
{
  auto & bm = frm()->GetBookmarkManager();
  bm.ResetLastSortingType(static_cast<kml::MarkGroupId>(catId));
}

JNIEXPORT jintArray JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetAvailableSortingTypes(JNIEnv *env,
    jobject, jlong catId, jboolean hasMyPosition)
{
  auto const & bm = frm()->GetBookmarkManager();
  auto const types =  bm.GetAvailableSortingTypes(static_cast<kml::MarkGroupId>(catId),
                                                  static_cast<bool>(hasMyPosition));
  int const size = static_cast<int>(types.size());
  jintArray jTypes = env->NewIntArray(size);
  jint * arr = env->GetIntArrayElements(jTypes, 0);
  for (int i = 0; i < size; ++i)
    arr[i] = static_cast<int>(types[i]);
  env->ReleaseIntArrayElements(jTypes, arr, 0);

  return jTypes;
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetSortedCategory(JNIEnv *env,
    jobject, jlong catId, jint sortingType, jboolean hasMyPosition, jdouble lat, jdouble lon,
    jlong timestamp)
{
  auto & bm = frm()->GetBookmarkManager();
  BookmarkManager::SortParams sortParams;
  sortParams.m_groupId = static_cast<kml::MarkGroupId>(catId);
  sortParams.m_sortingType = static_cast<BookmarkManager::SortingType>(sortingType);
  sortParams.m_hasMyPosition = static_cast<bool>(hasMyPosition);
  sortParams.m_myPosition = mercator::FromLatLon(static_cast<double>(lat),
      static_cast<double>(lon));
  sortParams.m_onResults = bind(&OnCategorySortingResults, env, timestamp, _1, _2);

  bm.GetSortedCategory(sortParams);
}

JNIEXPORT jstring JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetBookmarkName(
  JNIEnv * env, jclass, jlong bmk)
{
  auto const * mark = getBookmark(bmk);
  return jni::ToJavaString(env, mark != nullptr ? mark->GetPreferredName() : "");
}

JNIEXPORT jstring JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetBookmarkFeatureType(
  JNIEnv * env, jclass, jlong bmk)
{
  auto const * mark = getBookmark(bmk);
  return jni::ToJavaString(env, mark != nullptr ?
    kml::GetLocalizedFeatureType(mark->GetData().m_featureTypes) : "");
}

JNIEXPORT jstring JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetBookmarkDescription(
  JNIEnv * env, jclass, jlong bmk)
{
  auto const * mark = getBookmark(bmk);
  return jni::ToJavaString(env, mark != nullptr ? mark->GetDescription() : "");
}

JNIEXPORT jint JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetBookmarkColor(
  JNIEnv *, jclass, jlong bmk)
{
  auto const * mark = getBookmark(bmk);
  return static_cast<jint>(mark != nullptr ? mark->GetColor()
                                           : frm()->LastEditedBMColor());
}

JNIEXPORT jint JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetBookmarkIcon(
  JNIEnv *, jclass, jlong bmk)
{
  auto const * mark = getBookmark(bmk);
  return static_cast<jint>(mark != nullptr ? mark->GetData().m_icon
                                           : kml::BookmarkIcon::None);
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeSetBookmarkParams(
  JNIEnv * env, jclass, jlong bmk,
  jstring name, jint color, jstring descr)
{
  auto const * mark = getBookmark(bmk);

  // initialize new bookmark
  kml::BookmarkData bmData(mark->GetData());
  auto const bmName = jni::ToNativeString(env, name);
  if (mark->GetPreferredName() != bmName)
    kml::SetDefaultStr(bmData.m_customName, bmName);
  if (descr)
    kml::SetDefaultStr(bmData.m_description, jni::ToNativeString(env, descr));
  bmData.m_color.m_predefinedColor = static_cast<kml::PredefinedColor>(color);

  g_framework->ReplaceBookmark(static_cast<kml::MarkId>(bmk), bmData);
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeChangeBookmarkCategory(
  JNIEnv *, jclass, jlong oldCat, jlong newCat, jlong bmk)
{
  g_framework->MoveBookmark(static_cast<kml::MarkId>(bmk), static_cast<kml::MarkGroupId>(oldCat),
                            static_cast<kml::MarkGroupId>(newCat));
}

JNIEXPORT jobject JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetBookmarkXY(
  JNIEnv * env, jclass, jlong bmk)
{
  auto const * mark = getBookmark(bmk);
  return jni::GetNewParcelablePointD(env, mark != nullptr ? mark->GetPivot() : mercator::FromLatLon(0, 0));
}

JNIEXPORT jdouble JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetBookmarkScale(
  JNIEnv *, jclass, jlong bmk)
{
  auto const * mark = getBookmark(bmk);
  return mark != nullptr ? mark->GetScale() : scales::GetUpperComfortScale();
}

JNIEXPORT jstring JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeEncode2Ge0Url(
  JNIEnv * env, jclass, jlong bmk, jboolean addName)
{
  auto const * mark = getBookmark(bmk);
  return jni::ToJavaString(env, mark != nullptr ? frm()->CodeGe0url(mark, addName) : "");
}

JNIEXPORT jstring JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetBookmarkAddress(
  JNIEnv * env, jclass, jlong bmkId)
{
  auto const * mark = getBookmark(bmkId);
  if (mark == nullptr) {
    return jni::ToJavaString(env, "");
  }
  auto const address = frm()->GetAddressAtPoint(mark->GetPivot()).FormatAddress();
  return jni::ToJavaString(env, address);
}

JNIEXPORT jdouble JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetElevationCurPositionDistance(
    JNIEnv *, jclass, jlong trackId)
{
  auto const & bm = frm()->GetBookmarkManager();
  return static_cast<jdouble>(bm.GetElevationMyPosition(static_cast<kml::TrackId>(trackId)));
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeSetElevationCurrentPositionChangedListener(
        JNIEnv * env, jclass)
{
  frm()->GetBookmarkManager().SetElevationMyPositionChangedCallback(
      std::bind(&OnElevationCurPositionChanged, env));
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeRemoveElevationCurrentPositionChangedListener(
        JNIEnv *, jclass)
{
  frm()->GetBookmarkManager().SetElevationMyPositionChangedCallback(nullptr);
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeSetElevationActivePoint(
  JNIEnv *, jclass, jlong trackId, jdouble distanceInMeters)
{
  auto & bm = frm()->GetBookmarkManager();
  bm.SetElevationActivePoint(static_cast<kml::TrackId>(trackId),
                             static_cast<double>(distanceInMeters));
}

JNIEXPORT jdouble JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeGetElevationActivePointDistance(
  JNIEnv *, jclass, jlong trackId)
{
  auto & bm = frm()->GetBookmarkManager();
  return static_cast<jdouble>(bm.GetElevationActivePoint(static_cast<kml::TrackId>(trackId)));
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeSetElevationActiveChangedListener(
   JNIEnv *env, jclass)
{
  frm()->GetBookmarkManager().SetElevationActivePointChangedCallback(std::bind(&OnElevationActivePointChanged, env));
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeRemoveElevationActiveChangedListener(
        JNIEnv *, jclass)
{
  frm()->GetBookmarkManager().SetElevationActivePointChangedCallback(nullptr);
}

JNIEXPORT jboolean JNICALL
Java_app_organicmaps_widget_placepage_PlacePageButtonFactory_nativeHasRecentlyDeletedBookmark(JNIEnv *, jclass)
{
  return frm()->GetBookmarkManager().HasRecentlyDeletedBookmark();
}

/**
 * Add by RobinChien at 2020/07/07
 * Refactoring Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeAddBookmarkToLastEditedCategory
 * */
JNIEXPORT jlong JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeAddBookmark(
        JNIEnv * env, jobject thiz, jlong groupId, jstring name, jstring description, jint color, double lat, double lon, jint iconType)
{

  BookmarkManager & bmMng = frm()->GetBookmarkManager();

  kml::BookmarkData bmData;
  kml::LocalizableString bmName;
  kml::SetDefaultStr(bmName, ToNativeString(env, name));
  bmData.m_name = bmName;

  kml::LocalizableString bmDescription;
  kml::SetDefaultStr(bmDescription, ToNativeString(env, description));
  bmData.m_description = bmDescription;

  if (iconType == 0) {
    bmData.m_icon = kml::BookmarkIcon::Start;
  } else if(iconType == 1) {
    bmData.m_icon = kml::BookmarkIcon::Finish;
  } else {
    bmData.m_icon = kml::BookmarkIcon::None;
  }

  bmData.m_color.m_predefinedColor = kml::PredefinedColor(color);
  bmData.m_point = mercator::FromLatLon(lat, lon);
  auto *bookmark = bmMng.GetEditSession().CreateBookmark(std::move(bmData),
                                                                       static_cast<kml::MarkGroupId>(groupId));
  if (bookmark == nullptr) {
      return LLONG_MAX;
  }

  bookmark->SetScale(MAX(scales::GetUpperComfortScale(), frm()->GetDrawScale()));
  return static_cast<jlong>(bookmark->GetId());
}

/**
 * Add by RobinChien at 2020/07/09
 * Add method to search cateory ID with name
 * */
JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeUpdateBookmark(
        JNIEnv * env, jobject thiz, jlong bookmarkID, jstring name, jstring description, jint color, double lat, double lon)
{
//    BookmarkManager & bmMng = frm()->GetBookmarkManager();

  kml::BookmarkData bmData;
  kml::LocalizableString bmName;
  kml::SetDefaultStr(bmName, ToNativeString(env, name));
  bmData.m_name = bmName;

  kml::LocalizableString bmDescription;
  kml::SetDefaultStr(bmDescription, ToNativeString(env, description));
  bmData.m_description = bmDescription;

  bmData.m_color.m_predefinedColor = kml::PredefinedColor(color);
  bmData.m_point = mercator::FromLatLon(lat, lon);

  frm()->GetBookmarkManager().GetEditSession().UpdateBookmark(static_cast<kml::MarkId>(bookmarkID),
                                                              std::move(bmData));
}

/**
 * Add by RobinChien at 2020/07/09
 * Add method to delete all bookmark in category
 * */
JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeDeleteAllBookmarkWithCategory(
        JNIEnv * env, jobject thiz, jlong catId)
{
  auto const bookmarkIDs = frm()->GetBookmarkManager().GetUserMarkIds(
          static_cast<kml::MarkGroupId>(catId));
  for (auto const bookmarkID : bookmarkIDs) {
    frm()->GetBookmarkManager().GetEditSession().DeleteBookmark(static_cast<kml::MarkId>(bookmarkID));
  }
}

/**
 * Add by RobinChien at 2020/07/09
 * Add method to search bookmark ID with name
 * */
JNIEXPORT jlong JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeSearchBookmarkIDWithName(
        JNIEnv * env, jobject thiz, jstring bookmarkName, jlong catId)
{
  if (!bookmarkName)
    return LLONG_MAX;

  auto const bookmarkIDs = frm()->GetBookmarkManager().GetUserMarkIds(
          static_cast<kml::MarkGroupId>(catId));
  for (auto const bookmarkID : bookmarkIDs) {
    auto bookmark = frm()->GetBookmarkManager().GetBookmark(bookmarkID);
    if (!bookmark) {
      continue;
    }
    if (bookmark->GetPreferredName() == ToNativeString(env, bookmarkName)) {
      return bookmarkID;
    }
  }
  return LLONG_MAX;
}

/**
 * Add by RobinChien at 2020/07/09
 * Add method to search category ID with name
 * */
JNIEXPORT jlong JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeSearchCategoryIDWithName(
        JNIEnv * env, jobject thiz, jstring name)
{
  if (!name)
    return LLONG_MAX;

  auto const categoryID = frm()->GetBookmarkManager().GetCategoryId(ToNativeString(env, name));
  return (categoryID != kml::kInvalidMarkGroupId) ? categoryID : LLONG_MAX;
}

JNIEXPORT jlong JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeAddTrack(
        JNIEnv * env, jobject thiz, jlong catId, jstring name, jstring description, jobjectArray locations, jint color, double width)
{
  if (!frm()->GetBookmarkManager().HasBmCategory(catId)) {
    return kml::kInvalidTrackId;
  }

  kml::TrackData trackData;
  const jsize size = env->GetArrayLength(locations);

  std::vector<m2::PointD> points;
  for (jsize i = 0; i < size; ++i) {
    jobject jlocationArray = env->GetObjectArrayElement(locations, i);
    jdoubleArray * latlonData = reinterpret_cast<jdoubleArray *>(&jlocationArray);
    double * latlon = env->GetDoubleArrayElements(* latlonData, NULL);
    double lat = latlon[0];
    double lon = latlon[1];
    m2::PointD const point(mercator::FromLatLon(lat, lon));

    auto shouldAdd = true;
    if (!points.empty()) {
        auto lastPoint = points.back();
        shouldAdd = lastPoint.x != point.x || lastPoint.y != point.y;
    }
    if (shouldAdd) {
        points.emplace_back(geometry::PointWithAltitude(point, 0));
    }
    env->DeleteLocalRef(jlocationArray);
  }
  if (points.size() < 2) {
    return kml::kInvalidTrackId;
  }
  trackData.m_geometry.FromPoints(points);

  trackData.m_timestamp = std::chrono::time_point<std::chrono::system_clock>::max();

  kml::LocalizableString trackName;
  kml::SetDefaultStr(trackName, ToNativeString(env, name));
  trackData.m_name = trackName;

  kml::LocalizableString trackDescription;
  kml::SetDefaultStr(trackDescription, ToNativeString(env, description));
  trackData.m_description = trackDescription;

  kml::ColorData colorData;
  colorData.m_predefinedColor = kml::PredefinedColor(color);

  dp::Color dpcolor = initDpColor(color);
  colorData.m_rgba = dpcolor.GetRed() << 24 | dpcolor.GetGreen() << 16 | dpcolor.GetBlue() << 8 | dpcolor.GetAlpha();

  kml::TrackLayer trackLayer;
  trackLayer.m_color = colorData;
  trackLayer.m_lineWidth = width;
  trackData.m_layers.emplace_back(trackLayer);

  auto editSession = frm()->GetBookmarkManager().GetEditSession();
  auto track = editSession.CreateTrack(std::move(trackData));
  if (track == nullptr) {
    return kml::kInvalidTrackId;
  }
  long trackId = track->GetId();
  editSession.AttachTrack(trackId, catId);

  return trackId;
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeDeleteAllTracksInCategory(
        JNIEnv * env, jobject thiz, jlong catId)
{
  auto const trackIds = frm()->GetBookmarkManager().GetTrackIds(static_cast<kml::MarkGroupId>(catId));
  for (auto const trackId : trackIds) {
    frm()->GetBookmarkManager().GetEditSession().DeleteTrack(trackId);
  }
}

JNIEXPORT jint JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeDrawLineWithLocations(
        JNIEnv * env, jobject thiz, jobjectArray locations, jint color, double width)
{
  const jsize size = env->GetArrayLength(locations);
  std::vector<m2::PointD> points;
  for (jsize i = 0; i < size; ++i) {
    auto jlocationArray = (env->GetObjectArrayElement(locations, i));
    jdouble * latlonData = env->GetDoubleArrayElements((jdoubleArray) jlocationArray, 0);
    double lat = latlonData[0];
    double lon = latlonData[1];
    m2::PointD const point(mercator::FromLatLon(lat, lon));

    auto shouldAdd = true;
    if (!points.empty()) {
      auto lastPoint = points.back();
      shouldAdd = lastPoint.x != point.x || lastPoint.y != point.y;
    }
    if (shouldAdd) {
      points.emplace_back(geometry::PointWithAltitude(point, 0));
    }

    env->DeleteLocalRef(jlocationArray);
  }
  if (points.size() < 2) {
      return 0;
  }

  dp::Color dpcolor = initDpColor(color);

  lineId++;
  frm()->GetDrapeApi().AddLine(std::to_string(lineId), df::DrapeApiLineData(points, dpcolor).Width(width));

  return lineId;
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeRemoveLine(
        JNIEnv * env, jobject thiz, jint lineId)
{
  frm()->GetDrapeApi().RemoveLine(std::to_string(lineId));
}

JNIEXPORT void JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeClearLines(
        JNIEnv * env, jobject thiz)
{
  frm()->GetDrapeApi().Clear();
}

JNIEXPORT jint JNICALL
Java_app_organicmaps_bookmarks_data_BookmarkManager_nativeDrawCircle(JNIEnv * env, jobject thiz, jdouble lat, jdouble lon, jdouble radius, jint color, jdouble width)
{
  std::vector<m2::PointD> points = ms::CreateCircleGeometryOnEarth(ms::LatLon(static_cast<double>(lat), static_cast<double>(lon)), radius, 1);

  lineId++;
  frm()->GetDrapeApi().AddLine(std::to_string(lineId), df::DrapeApiLineData(points, initDpColor(color)).Width(static_cast<double>(width)));
  return lineId;
}

}  // extern "C"
