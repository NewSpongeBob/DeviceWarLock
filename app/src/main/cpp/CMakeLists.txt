cmake_minimum_required(VERSION 3.22.1)
project("warlockCore")

# 设置 RapidJSON 选项，禁用构建示例和测试
set(RAPIDJSON_BUILD_TESTS OFF CACHE BOOL "Build rapidjson tests" FORCE)
set(RAPIDJSON_BUILD_EXAMPLES OFF CACHE BOOL "Build rapidjson examples" FORCE)
set(RAPIDJSON_BUILD_DOC OFF CACHE BOOL "Build rapidjson documentation" FORCE)

# 下载 RapidJSON
include(FetchContent)
FetchContent_Declare(
        rapidjson
        GIT_REPOSITORY https://github.com/Tencent/rapidjson.git
        GIT_TAG master
)
FetchContent_MakeAvailable(rapidjson)

# 设置源文件
set(SOURCE_FILES
        src/warlockCore.cpp
        src/collector/NativeCollector.cpp
        src/collector/SystemInfoCollector.cpp
        src/collector/BasicInfoCollector.cpp
        src/collector/MiscInfoCollector.cpp
        src/utils/CommandUtils.cpp
        src/utils/FileUtils.cpp
        src/utils/StringUtils.cpp
        src/utils/SyscallUtils.cpp
        src/utils/Base64Utils.cpp
        src/utils/XsonCollector.cpp
)

# 添加库
add_library(warlockCore SHARED ${SOURCE_FILES})

# 添加头文件目录
include_directories(inc/)

# 查找需要的库
find_library(log-lib log)
find_library(mediandk-lib mediandk)

# 添加 RapidJSON 头文件路径
target_include_directories(warlockCore PRIVATE ${rapidjson_SOURCE_DIR}/include)

# 链接库
target_link_libraries(warlockCore
        ${log-lib}
        ${mediandk-lib}
)