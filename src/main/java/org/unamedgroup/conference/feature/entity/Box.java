package org.unamedgroup.conference.feature.entity;


/**
 * 人脸检测类
 * @author zhoutao
 * @date 2019/05/31
 */
import static java.lang.Math.max;


public class Box {
    public int[] box;
    public float score;
    public float[] bbr;
    public boolean deleted;

    Box() {
        box = new int[4];
        bbr = new float[4];
        deleted = false;
    }

    public int left() {
        return box[0];
    }

    public int right() {
        return box[2];
    }

    public int top() {
        return box[1];
    }

    public int bottom() {
        return box[3];
    }

    public int width() {
        return box[2] - box[0] + 1;
    }

    public int height() {
        return box[3] - box[1] + 1;
    }


    /**
     *  面积
     * @author zhoutao
     * @date 2019/05/31
     */
    public int area() {
        return width() * height();
    }

    /**
     * Bounding Box Regression， 将偏移并入总的坐标中
     * @author zhoutao
     * @date 2019/05/31
     */
    public void calibrate() {
        // 获取人脸框的宽与高
        int w = box[2] - box[0] + 1;
        int h = box[3] - box[1] + 1;
        // 偏移并入人脸框
        box[0] = (int) (box[0] + w * bbr[0]);
        box[1] = (int) (box[1] + h * bbr[1]);
        box[2] = (int) (box[2] + w * bbr[2]);
        box[3] = (int) (box[3] + h * bbr[3]);
        // 修改偏移全部为0
        for (int i = 0; i < 4; i++) {
            bbr[i] = 0.0f;
        }
    }

    /**
     * 将当前的Box转换成正方形
     * @author zhoutao
     * @date 2019/05/31
     */
    public void toSquareShape() {
        int w = width();
        int h = height();
        if (w > h) {
            box[1] -= (w - h) / 2;
            box[3] += (w - h + 1) / 2;
        } else {
            box[0] -= (h - w) / 2;
            box[2] += (h - w + 1) / 2;
        }
    }

    /**
     * 防止边界溢出，并维持square大小
     * @author zhoutao
     * @date 2019/05/31
     */
    public void limitSquare(int w, int h) {
        if (box[0] < 0 || box[1] < 0) {
            int len = max(-box[0], -box[1]);
            box[0] += len;
            box[1] += len;
        }
        if (box[2] >= w || box[3] >= h) {
            int len = max(box[2] - w + 1, box[3] - h + 1);
            box[2] -= len;
            box[3] -= len;
        }
    }
}