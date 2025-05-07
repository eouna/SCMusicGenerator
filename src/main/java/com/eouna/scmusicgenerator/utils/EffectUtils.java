package com.eouna.scmusicgenerator.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.commons.lang3.RandomUtils;

/**
 * 效果类工具
 *
 * @author CCL
 */
public class EffectUtils {
  public static class ShakeMeta {
    /** 爆炸点X */
    public double explosionX;

    /** 爆炸点Y */
    public double explosionY;

    /** 每秒震动的时间ms */
    public int durationPerShake;

    /** 爆炸次数 */
    public int shakeCount;

    /** 爆炸力度 */
    public int explosionStrength;

    public ShakeMeta(
        double explosionX,
        double explosionY,
        int durationPerShake,
        int shakeCount,
        int explosionStrength) {
      this.explosionX = explosionX;
      this.explosionY = explosionY;
      this.durationPerShake = durationPerShake;
      this.shakeCount = shakeCount;
      this.explosionStrength = explosionStrength;
    }
  }

  /**
   * 窗口摇晃
   *
   * @param stage stage
   */
  public static void shakeWindow(Stage stage, ShakeMeta shakeMeta) {

    double originalX = stage.getX();
    double originalY = stage.getY();

    // 计算窗口中心与爆炸点的距离
    double windowCenterX = originalX + stage.getWidth() / 2;
    double windowCenterY = originalY + stage.getHeight() / 2;
    double dx = windowCenterX - shakeMeta.explosionX;
    double dy = windowCenterY - shakeMeta.explosionY;
    double distance = Math.sqrt(dx * dx + dy * dy);

    // 模拟爆炸力衰减 防止除 0
    double force = shakeMeta.explosionStrength / (distance * distance + 1);

    // 将力转化为最大抖动位移（限制最大值）
    double maxOffset = Math.min(force, 30);

    Timeline timeline = new Timeline();
    for (int i = 0; i < shakeMeta.shakeCount; i++) {
      double angle = Math.random() * 2 * Math.PI;
      double offsetX = Math.cos(angle) * maxOffset * Math.random();
      double offsetY = Math.sin(angle) * maxOffset * Math.random();

      KeyFrame frame =
          new KeyFrame(
              Duration.millis(i * shakeMeta.durationPerShake),
              e -> {
                stage.setX(originalX + offsetX);
                stage.setY(originalY + offsetY);
              });
      timeline.getKeyFrames().add(frame);
    }

    // 回归原位
    timeline
        .getKeyFrames()
        .add(
            new KeyFrame(
                Duration.millis(shakeMeta.shakeCount * shakeMeta.durationPerShake),
                e -> {
                  stage.setX(originalX);
                  stage.setY(originalY);
                }));

    timeline.play();
  }

  public static class SimpleFireworkEffect {

    List<Particle> particles = new ArrayList<>();
    Random random = new Random();

    /** 每个烟花粒子存活时间 */
    final byte perFireworkAliveTime = 60;

    Pane pane;
    int timePlayGap;
    boolean autoPlay;
    int playCountPerFrame = 1;
    long startPlayTime;

    /** 每个烟花的粒子个数 */
    int particleDensity = 120;

    public SimpleFireworkEffect(Pane pane, int frameRatio) {
      this.pane = pane;
      this.timePlayGap = 1000 / frameRatio;
    }

    public SimpleFireworkEffect(Pane pane, int frameRatio, boolean autoPlay, int playCount) {
      this.pane = pane;
      this.timePlayGap = 1000 / frameRatio;
      this.autoPlay = autoPlay;
      this.playCountPerFrame = playCount;
    }

    public AnimationTimer playFireworks(Node node) {
      Canvas canvas = new Canvas(pane.getPrefWidth(), pane.getPrefHeight());
      GraphicsContext gc = canvas.getGraphicsContext2D();
      canvas.setOpacity(0.6);
      pane.getChildren().add(0, canvas);

      // 鼠标点击触发烟花爆炸
      node.setOnMouseClicked(e -> createFirework(e.getX(), e.getY()));

      // 动画循环
      return new AnimationTimer() {
        @Override
        public void handle(long now) {
          if (startPlayTime == 0 || now - startPlayTime >= playCountPerFrame) {
            buildFireWork();
            startPlayTime = now;
            updateParticles();
            render(gc);
          }
        }
      };
    }

    private void buildFireWork() {
      if (!autoPlay) {
        return;
      }
      /*int createCount =
          Math.max(
              0, playCountPerFrame - (int) Math.ceil(particles.size() / (particleDensity * 1.0)));*/
      int createCount = playCountPerFrame;
      double panelX = pane.getPrefWidth(), panelY = pane.getPrefHeight();
      for (int i = 0; i < createCount; i++) {
        double locX = RandomUtils.nextDouble(0, panelX);
        double locY = RandomUtils.nextDouble(0, panelY);
        createFirework(locX, locY);
      }
    }

    private void createFirework(double x, double y) {
      for (int i = 0; i < particleDensity; i++) {
        double angle = 2 * Math.PI * i / particleDensity;
        double speed = 2 + random.nextDouble() * 0.8;
        double vx = Math.cos(angle) * speed;
        double vy = Math.sin(angle) * speed;
        Color color = Color.hsb(random.nextDouble() * 360, 1.0, 1.0);
        particles.add(new Particle(x, y, vx, vy, color));
      }
    }

    private void updateParticles() {
      Iterator<Particle> iterator = particles.iterator();
      while (iterator.hasNext()) {
        Particle p = iterator.next();
        p.update();
        if (!p.isAlive()) {
          iterator.remove();
        }
      }
    }

    private void render(GraphicsContext gc) {
      gc.setFill(new Color(1, 1, 1, 0.2));
      gc.fillRect(0, 0, pane.getWidth(), pane.getHeight());

      // 绘制每个粒子的光晕
      for (Particle p : particles) {
        double haloSize = 2;
        Color glow = p.color.deriveColor(0, 1, 1, p.alpha * 0.5);
        gc.setFill(glow);
        // 模拟光晕
        gc.fillOval(p.x - haloSize / 2, p.y - haloSize / 2, haloSize, haloSize);

        // 粒子主体
        gc.setFill(p.color.deriveColor(0, 1, 1, p.alpha));
        gc.fillOval(p.x, p.y, 3, 3);
      }
    }

    // 内部类：粒子
    private static class Particle {
      double x, y;
      double vx, vy;
      double alpha = 1.0;
      Color color;

      public Particle(double x, double y, double vx, double vy, Color color) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.color = color;
      }

      public void update() {
        x += vx;
        y += vy;
        // 模拟重力
        vy += 0.04;
        // 慢慢消失
        alpha -= 0.015;
      }

      public boolean isAlive() {
        return alpha > 0;
      }
    }
  }
}
