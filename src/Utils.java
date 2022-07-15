public class Utils {
    public static Matrix.m4x4 eye4x4() {
        return new Matrix.m4x4(
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
    }

    public static Matrix.m4x4 rotX(double angle) {
        return new Matrix.m4x4(1, 0, 0, 0,
                0,  Math.cos(Math.toRadians(angle)), -Math.sin(Math.toRadians(angle)), 0,
                0, Math.sin(Math.toRadians(angle)), Math.cos(Math.toRadians(angle)), 0,
                0, 0, 0, 1);
    }

    public static Matrix.m4x4 rotY(double angle) {
        return new Matrix.m4x4(Math.cos(Math.toRadians(angle)), 0, Math.sin(Math.toRadians(angle)), 0,
                0, 1, 0, 0,
                -Math.sin(Math.toRadians(angle)), 0, Math.cos(Math.toRadians(angle)), 0,
                0, 0, 0, 1);
    }

    public static Matrix.m4x4 rotZ(double angle) {
        return new Matrix.m4x4(Math.cos(Math.toRadians(angle)), -Math.sin(Math.toRadians(angle)), 0, 0,
                Math.sin(Math.toRadians(angle)), Math.cos(Math.toRadians(angle)), 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
    }

    public static Matrix.m4x4 rotate(Point3D rot) {
        return Utils.rotZ(rot.getZ()).multiply(Utils.rotY(rot.getY())).multiply(Utils.rotX(rot.getX()));
    }

    public static Matrix.m4x4 move(double x, double y, double z) {
        return new Matrix.m4x4(1, 0, 0, x,
                0, 1, 0, y,
                0, 0, 1, z,
                0, 0, 0, 1);
    }

    public static Matrix.m4x4 move(Point3D pos) {
        return Utils.move(pos.getX(), pos.getY(), pos.getZ());
    }

    public static Matrix.m4x4 scale(double x, double y, double z) {
        return new Matrix.m4x4(x, 0, 0, 0,
                0, y, 0, 0,
                0, 0, z, 0,
                0, 0, 0, 1);
    }

    public static Matrix.m4x4 projection(double FOV, double aspect, double zFar, double zNear) {
        return new Matrix.m4x4((aspect/Math.tan(Math.toRadians(FOV/2))), 0, 0, 0,
                0, (1/Math.tan(Math.toRadians(FOV/2))), 0, 0,
                0, 0, zFar/(zFar-zNear), 1,
                0, 0,  -zFar*zNear/(zFar-zNear),0);
    }

    public static Matrix.m4x4 quaternionRotation(Quaternion quaternion) {
        double q0 = quaternion.getW();
        double q1 = quaternion.getX();
        double q2 = quaternion.getY();
        double q3 = quaternion.getZ();
        return new Matrix.m4x4(
                q0*q0+q1*q1-q2*q2-q3*q3, 2*(q1*q2-q0*q3), 2*(q0*q2+q1*q3), 0,
                2*(q1*q2+q0*q3), q0*q0-q1*q1+q2*q2-q3*q3, 2*(q2*q3-q0*q1), 0,
                2*(q1*q3-q0*q2), 2*(q0*q1+q2*q3), q0*q0-q1*q1-q2*q2+q3*q3, 0,
                0, 0, 0, 1
        );
    }

    public static Quaternion eulerAnglesToQuaternion(Point3D rotation) {
        double cy =  Math.cos(Math.toRadians(rotation.getZ()) * 0.5);
        double sy =  Math.sin(Math.toRadians(rotation.getZ()) * 0.5);
        double cp =  Math.cos(Math.toRadians(rotation.getY()) * 0.5);
        double sp =  Math.sin(Math.toRadians(rotation.getY()) * 0.5);
        double cr =  Math.cos(Math.toRadians(rotation.getX()) * 0.5);
        double sr =  Math.sin(Math.toRadians(rotation.getX()) * 0.5);

        return new Quaternion( cr * cp * cy + sr * sp * sy,sr * cp * cy - cr * sp * sy,cr * sp * cy + sr * cp * sy, cr * cp * sy - sr * sp * cy);
    }

    public static Point3D quaternionToEulerAngles(Quaternion quaternion) {
        double q0 = quaternion.getW();
        double q1 = quaternion.getX();
        double q2 = quaternion.getY();
        double q3 = quaternion.getZ();
        return new Point3D(
                Math.toDegrees(Math.atan2(2*(q0*q1+q2*q3),1-2*(q1*q1+q2*q2))),
                Math.toDegrees(Math.asin(2*(q0*q2-q3*q1))),
                Math.toDegrees(Math.atan2(2*(q0*q3+q1*q2),1-2*(q2*q2+q3*q3)))
        );
    }

    public static Quaternion normalToQuaternion(Point3D normal) {
        Point3D v1 = new Point3D(0,1, 0);
        Point3D v2 = normal;
        Point3D a = v1.cross(v2);
        Quaternion q = new Quaternion(Math.sqrt((v1.length()*v1.length()) * (v2.length()*v2.length())) + v1.dot(v2),a).normalise();
        return q;
    }
}
