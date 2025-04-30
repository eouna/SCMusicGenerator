package com.eouna.scmusicgenerator.constant;

/**
 * 配置常量
 *
 * @author CCL
 */
public interface DefaultEnvConfigConstant {
  // region============================== 软件信息 =============================
  /** 工具默认版本号 */
  String TOOL_DEFAULT_VERSION = "v1.0.1";

  /** 作者名 */
  String AUTHOR = "CCL";

  /** 公司名 */
  String COM_NAME = "eouna";

  // endregion============================== 软件信息 ==============================
  /** 系统配置路径 */
  String CONFIG_PATH = "./config/";
  /** 背景配置路径 */
  String BACKGROUND_IMG_CONFIG_PATH = CONFIG_PATH + "backgroundInfo.dat";

  /** 默认的音乐选择目录 */
  String DEFAULT_MUSIC_SELECT_PATH = "./music";

  /** 主界面默认背景图 */
  String DEFAULT_BACKGROUND_IMG = "img/hammer1.png";

  /** 日志区域显示最大条数 当前 */
  int LOG_AREA_MAX_SHOW_NUM = 1000;

  /** SC音乐模块最大能存储的字符 */
  int SC_MUSIC_BLOCK_MAX_STORAGE_CHAR = 256;

  /** 音乐数据展示区域默认透明度 */
  double DEFAULT_MUSIC_DATA_PANEL_OPACITY = 0.6;

  interface ColorDefine {
    /** 安全色 */
    String SAFE = "#00FF7F";

    /** 危险色 */
    String DANGER = "#F51717FF";
  }
}
