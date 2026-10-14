package com.eouna.scmusicgenerator.generator.midi;

/**
 * 乐器端(Tone)数据的生成方式.
 *
 * <p>发音器只在乐器端输入 0 -> 非 0 的上升沿时发声, 因此乐器端不能一直保持乐器码不变.
 *
 * @author CCL
 */
public enum ToneMode {
  /**
   * 每格都写乐器码. 接线时乐器端存储板的输出必须与时钟脉冲(电平 F)过一个 AND 门, 由时钟制造上升沿; 钢琴(7/F)可以不用存储板, 直接把时钟脉冲接到乐器端.
   */
  CONSTANT("恒定乐器码 (需与时钟AND)", 1),

  /**
   * 每一步占两格: 第一格写乐器码, 第二格写 0 (同时 P=F). 存储板输出本身就产生上升沿, 不需要 AND 门, 但一块板只能存 128 步, 时钟频率也要加倍.
   */
  PULSE("脉冲 (音后补0, 免AND门)", 2);

  private final String label;

  /** 每个量化步长占用的格子数 */
  private final int cellsPerStep;

  ToneMode(String label, int cellsPerStep) {
    this.label = label;
    this.cellsPerStep = cellsPerStep;
  }

  public String getLabel() {
    return label;
  }

  public int getCellsPerStep() {
    return cellsPerStep;
  }

  @Override
  public String toString() {
    return label;
  }
}
