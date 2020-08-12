package com.example.escaperoomcontroller;

public class Vector3D {
    private float x, y, z;

    Vector3D() {
        this.setZero();
    }

    Vector3D(final float x, final float y, final float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    Vector3D(final float[] values) {
        if (values.length != 3) {
            throw new Error("Values length must be 3.");
        }

        this.x = values[0];
        this.y = values[1];
        this.z = values[2];
    }

    Vector3D(final Vector3D vector3D) {
        this.x = vector3D.x;
        this.y = vector3D.y;
        this.z = vector3D.z;
    }

    Vector3D(final double x, final double y, final double z) {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
    }

    Vector3D add(final Vector3D v) {
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;

        return this;
    }

    Vector3D multiply(final float scalar) {
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;

        return this;
    }

    Vector3D setZero() {
        this.x = 0;
        this.y = 0;
        this.z = 0;

        return this;
    }

    void setX(float x) {
        this.x = x;
    }

    void setY(float y) {
        this.y = y;
    }

    void setZ(float z) {
        this.z = z;
    }

    float getX() {
        return x;
    }

    float getY() {
        return y;
    }

    float getZ() { return z; }

    float getDistance(final Vector3D other) {
        final float dx = this.x - other.x;
        final float dy = this.y - other.y;
        final float dz = this.z - other.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    float getLength() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    Vector3D getUnit() {
        final float len = getLength();
        if (len == 0) {
            return new Vector3D(0, 0, 0);
        }
        return new Vector3D(x / len, y / len, z / len);
    }

    Vector3D getMultipliedBy(final float scalar) {
        return new Vector3D(x * scalar, y * scalar, z * scalar);
    }

    Vector3D getSum(final Vector3D other) {
        return new Vector3D(x + other.x, y + other.y, z + other.z);
    }

    float dotProduct(final Vector3D other) {
        return this.x * other.x + this.y * other.y + this.z + other.z;
    }

    boolean isZero() {
        return this.x == 0 && this.y == 0 && this.z == 0;
    }

    @Override
    public String toString() {
        return String.format("(%s, %s, %s)", this.x, this.y, this.z);
    }
}
