import com.sun.scenario.effect.impl.prism.ps.PPSBlend_ADDPeer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.nio.channels.Pipe;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicReference;

public class MainFrame extends JFrame {
    Updater updater;
    Camera camera;
    ControlsGUI controlsGUI;
    Viewer3D viewer3D;
    JTextArea textArea;

    public MainFrame() {
        super("3D Viewer");
        updater = new Updater(80);
        camera = new Camera();
        controlsGUI = new ControlsGUI(camera);
        viewer3D = new Viewer3D(controlsGUI);
        textArea = new JTextArea("Hello World!");
        setup();
    }

    public static void main(String[] args) {
        MainFrame mainFrame = new MainFrame();
        mainFrame.updater.start();
        mainFrame.display();
        /*for(int i = 0; i <10000; i++) {
            mainFrame.updater.doTasks();
        }*/
    }

    public void setup() {
        camera.setPosition(new Point3D(0,0,-30));
        controlsGUI.bindWASD(camera, 0.7f);

        /////// TEST ZONE

        //создаем параметры пространства
        SimulationEnvironment simulationEnvironment = new SimulationEnvironment();

        //создаем пропеллеры
        double lPitch = 2.5;
        double lRoll = 2.5;
        AircraftBase.Propeller m1 = new AircraftBase.Propeller(0.1,new Point3D(lPitch,-0.3,lRoll),new Point3D(0,1,0),false,10,"propellerB.obj", simulationEnvironment);
        AircraftBase.Propeller m2 = new AircraftBase.Propeller(0.1,new Point3D(lPitch,-0.3,-lRoll),new Point3D(0,1,0),true,10,"propellerG.obj",simulationEnvironment);
        AircraftBase.Propeller m3 = new AircraftBase.Propeller(0.1,new Point3D(-lPitch,-0.3,-lRoll),new Point3D(0,1,0),false,10,"propellerY.obj",simulationEnvironment);
        AircraftBase.Propeller m4 = new AircraftBase.Propeller(0.1,new Point3D(-lPitch,-0.3,lRoll),new Point3D(0,1,0),true,10,"propellerM.obj",simulationEnvironment);
        //QuadCopter.Propeller m5 = new QuadCopter.Propeller(1,new Point3D(0,0,2),new Point3D(0,0,-1),true,1000,simulationEnvironment);

        //создаем сам коптер
        Matrix.m4x4 J = new Matrix.m4x4(0.64,0 ,0, 0, 0, 0.64, 0, 0, 0,0, 1.2,0, 0, 0 ,0 ,1 );
        AircraftBase copter = new AircraftBase(3,0.1, J, simulationEnvironment);
        //AircraftBase copter2 = new AircraftBase(3,0.1, J, simulationEnvironment);
        //добавляем пропеллеры на коптер
        copter.addPropeller(m1);
        copter.addPropeller(m2);
        copter.addPropeller(m3);
        copter.addPropeller(m4);
        //copter.addPropeller(m5);
        //m5.setSpeed(1);

        updater.addTask(copter::calculateForces);
        //добавляем коптер и пропеллеры в пространство
        viewer3D.addObject3D(copter.getCopterObject());
        for (int i = 0; i < copter.getPropellers().size(); i++) {
            viewer3D.addObject3D(copter.getPropellers().get(i).getObjectStick());
            viewer3D.addObject3D(copter.getPropellers().get(i).getPropeller3D());
        }

        //создаем и добавляем в пространство точку, в которурую должен прийти коптер
        Object3D OBJRef = new ReaderOBJ("arrow.obj").getObject();
        viewer3D.addObject3D(OBJRef);

        //добовляем следящую за коптером линию
        LineTracer lineTracer = new LineTracer(500,4,copter.getPosition());
        updater.addTask(()->lineTracer.updatePosition(copter.getPosition()));
        viewer3D.addObject3D(lineTracer.getLinesObject());

        ///////////////////////// CONTROLLER
        PID pid1 = new PID(0.5,0,1, simulationEnvironment);
        PID pid2 = new PID(0.5,0,1, simulationEnvironment);
        PID pid3 = new PID(0.5,0,1, simulationEnvironment);
        PID pid4 = new PID(3,0,0, simulationEnvironment);
        QuaternionPID qPID = new QuaternionPID(1,0,0,simulationEnvironment);


        copter.setAngle(Utils.eulerAnglesToQuaternion(new Point3D(0,0,0)));

        AtomicReference<Double> pitch = new AtomicReference<>(0.);
        AtomicReference<Quaternion> quaternion = new AtomicReference<>(Utils.eulerAnglesToQuaternion(new Point3D(0,0,0)));

        updater.addTask( () -> {
            quaternion.updateAndGet(q -> (q.multiply(Utils.eulerAnglesToQuaternion(new Point3D(0,0.1,0)))));
            Quaternion rotated = quaternion.get().multiply(Utils.eulerAnglesToQuaternion(new Point3D(78,0,32)).conjugate());
            //System.out.println(rotated.getX()+ " "+rotated.getY() + " " + rotated.getZ()+" "+rotated.getW());

        });

        updater.addTask( () -> {
            if (pitch.get()<360)
                pitch.updateAndGet(v->(v+0.05));
            Point3D eulerAngle = Utils.quaternionToEulerAngles(copter.getAngle());
            Quaternion angle = copter.getAngle().multiply(Utils.eulerAnglesToQuaternion(new Point3D(0,pitch.get(),0)).conjugate());
            //System.out.println(angle);
            //System.out.println(copter.getAngle());

            double anglePitch = Math.acos(angle.getW())*2;
            Point3D neededPosition = new Point3D();
            double neededAngle =0;// pitch.get();


            double err1 = pid1.calculateControl(-copter.getPosition().getY(),0);
            double err2 = pid2.calculateControl(0,0);
            double err3 = pid3.calculateControl(0,0);
            double err4 = pid4.calculateControl(anglePitch,0);
            Point3D qerr =qPID.calculateControl(copter.getAngle(),Utils.eulerAnglesToQuaternion(new Point3D(0,pitch.get(),0)));


            Quaternion e = angle.normalise().multiply(2*Math.atan2(angle.getVector().length(),angle.getW()));
            System.out.println(e.getX()+ " "+e.getY() + " " + e.getZ()+" "+e.getW()+" "+pitch.get());

            //System.out.println(Math.toDegrees(anglePitch)+" "+pitch.get());

            m1.setSpeed(err1-err4+err2-err3);
            m2.setSpeed(err1+err4-err2-err3);
            m3.setSpeed(err1-err4-err2+err3);
            m4.setSpeed(err1+err4+err2+err3);

        });
        ///////////////////////// CONTROLLER END


        //обновляем управление
        updater.addTask(controlsGUI::updateControls);

        //обнавляем пропеллеры
        updater.addTask(() -> {
            for (AircraftBase.Propeller propeller : copter.getPropellers())
                propeller.updateThrust();
        });

        //обналяем и интегрируем силы коптера
        updater.addTask(copter::updateIntegration);

        //управление с клавиатуры
        controlsGUI.bindAKey(KeyEvent.VK_SHIFT, () -> {
            m1.setSpeed(m1.getThrust()-1.1f);
            m2.setSpeed(m2.getThrust()+1.1f);
            m3.setSpeed(m3.getThrust()-1.1f);
            m4.setSpeed(m4.getThrust()+1.1f);
        });
        controlsGUI.bindAKey(KeyEvent.VK_CONTROL, () -> {
            m1.setSpeed(m1.getThrust()+1.1f);
            m2.setSpeed(m2.getThrust()-1.1f);
            m3.setSpeed(m3.getThrust()+1.1f);
            m4.setSpeed(m4.getThrust()-1.1f);
        });
        controlsGUI.bindAKey(KeyEvent.VK_LEFT, () -> {
            m1.setSpeed(m1.getThrust()+1.1f);
            m2.setSpeed(m2.getThrust()-1.1f);
            m3.setSpeed(m3.getThrust()-1.1f);
            m4.setSpeed(m4.getThrust()+1.1f);
        });
        controlsGUI.bindAKey(KeyEvent.VK_UP, () -> {
            m1.setSpeed(m1.getThrust()-1.1f);
            m2.setSpeed(m2.getThrust()-1.1f);
            m3.setSpeed(m3.getThrust()+1.1f);
            m4.setSpeed(m4.getThrust()+1.1f);
        });
        controlsGUI.bindAKey(KeyEvent.VK_RIGHT, () -> {
            m1.setSpeed(m1.getThrust()-1.1f);
            m2.setSpeed(m2.getThrust()+1.1f);
            m3.setSpeed(m3.getThrust()+1.1f);
            m4.setSpeed(m4.getThrust()-1.1f);
        });
        controlsGUI.bindAKey(KeyEvent.VK_DOWN, () -> {
            m1.setSpeed(m1.getThrust()+1.1f);
            m2.setSpeed(m2.getThrust()+1.1f);
            m3.setSpeed(m3.getThrust()-1.1f);
            m4.setSpeed(m4.getThrust()-1.1f);
        });
        controlsGUI.bindAKey(KeyEvent.VK_N, () -> {
            m1.setSpeed(0);
            m2.setSpeed(0);
            m3.setSpeed(0);
            m4.setSpeed(0);
        });

        // поворот камеры при зажатой кнопке M
        controlsGUI.bindAKey(KeyEvent.VK_M,
                () -> camera.setPosition(copter.getPosition().multiply(-1).add(
                        new Point3D((20*Math.sin(Math.toRadians(camera.getRotation().getY()))*Math.cos(Math.toRadians(camera.getRotation().getX()))),
                                (-20*Math.sin(Math.toRadians(camera.getRotation().getX()))),
                                (-20*Math.cos(Math.toRadians(camera.getRotation().getY()))*Math.cos(Math.toRadians(camera.getRotation().getX())))))));
        // Cam rotation end

        //обнавляем позицию объектов коптера и пропеллеров
        updater.addTask(copter::updateObjectAndPropellers);

        //выводим FPS
        updater.addTask(() -> {
            textArea.setText(String.valueOf(updater.getFrames()));
        });

        ///////// END OF TEST ZONE

        //обнавляем отрисовку объектов
        updater.addTask(viewer3D::updateComponent);
    }

    void display() {
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setBackground(Color.BLACK);

        textArea.setEditable(false);
        textArea.setBackground(Color.BLACK);
        textArea.setFont( new Font(Font.DIALOG, Font.PLAIN, 30 ));
        textArea.setForeground(Color.WHITE);

        //add(textArea, BorderLayout.PAGE_START);
        add(viewer3D, BorderLayout.CENTER);

        viewer3D.setFocusable(true);
        viewer3D.grabFocus();

        setVisible(true);
    }
}
