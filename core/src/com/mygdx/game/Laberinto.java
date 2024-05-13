package com.mygdx.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Disposable;
import com.mygdx.game.enums.CameraMode;

public class Laberinto extends ApplicationAdapter implements InputProcessor {

    class MyContactListener extends ContactListener {
        @Override
        public boolean onContactAdded (int userValue0, int partId0, int index0, int userValue1, int partId1, int index1) {
           // System.out.println("ENTRO");
           // System.out.println("userValue0: " + userValue0 + " partId0:" + partId0 + " index0 "+ index0 );
           // System.out.println("userValue1: " + userValue1 + " partId1:" + partId1 + " index1 "+ index1 );
           // System.out.println(instances.get(index0).transform.);
            System.out.println("ANGLE: " + angleBehindPlayer);
            if(angleBehindPlayer != 90 && angleBehindPlayer != -90){
                instances.get(userValue0).moving = false;
                //instances.get()
                instances.get(userValue1).moving = false;
            }else{
                instances.get(userValue0).moving = true;
                //instances.get()
                instances.get(userValue1).moving = true;
            }
            return true;
        }
    }
    static class GameObject extends ModelInstance implements Disposable {
        public final btCollisionObject body;
        public boolean moving;

        public GameObject(Model model, btCollisionShape shape){
            super(model);
            this.body = new btCollisionObject();
            body.setCollisionShape(shape);
        }
        public GameObject(Model model, String node, btCollisionShape shape) {
            super(model, node);
            this.body = new btCollisionObject();
            body.setCollisionShape(shape);
        }

        @Override
        public void dispose() {
            body.dispose();
        }

        static class Constructor implements Disposable {

            public final Model model;
            public final String node;
            public final btCollisionShape shape;

            public Constructor(Model model, String node, btCollisionShape shape) {
                this.model = model;
                this.node = node;
                this.shape = shape;
            }
            public Constructor(Model model,btCollisionShape shape){
                this.model = model;
                this.shape = shape;
                this.node = null;
            }

            public GameObject construct() {
                return (node != null)? new GameObject(model, node, shape): new GameObject(model, shape);
            }

            @Override
            public void dispose() {
                shape.dispose();
            }
        }
    }

    PerspectiveCamera cam;
    CameraInputController camController;
    ModelBatch modelBatch;
    Array<GameObject> instances;
    ArrayMap<String, GameObject.Constructor> constructors;

    Environment environment;

    Model model;
    ModelInstance ground;
    ModelInstance ball;

    boolean collsion;
    btCollisionShape groundShape;
    btCollisionShape ballShape;
    btCollisionObject groundObject;
    btCollisionObject ballObject;
    float spawnTimer;

    btCollisionConfiguration collisionConfig;
    btDispatcher dispatcher;
    MyContactListener contactListener;

    btBroadphaseInterface broadphase;
    btCollisionWorld collisionWorld;

    boolean moveUp = true;
    boolean moveDown  = true;


    private Matrix4 playerTransform = new Matrix4();
    private final Vector3 moveTranslation = new Vector3();
    float speed = 5f;
    private float distanceFromPlayer = 35f;
    private float camPitch = Settings.CAMERA_START_PITCH;
    private float angleAroundPlayer  = 0f;
    private CameraMode cameraMode = CameraMode.BEHIND_PLAYER;
    private float angleBehindPlayer = 0f;
    private final Vector3 currentPosition = new Vector3();

    float rotationspeed = 80f;
    float minim_wall = (float) Math.sqrt(Math.pow(15, 2) + Math.pow(7.5,2));
    float size  = (float) (120/Math.cos(Math.toRadians(30)));
    //Player
    ModelInstance player;

    @Override
    public void create() {

        modelBatch = new ModelBatch();
        Bullet.init();
        contactListener = new MyContactListener();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        cam = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.near = 1f;
        cam.far = 200;
        cam.position.set(0, 0, 4f);
        //cam.lookAt(0, 0, 0);
        cam.update();

        //camController = new CameraInputController(cam);
        //Gdx.input.setInputProcessor(camController);
        Gdx.input.setCursorCatched(true);
        Gdx.input.setInputProcessor(this);


        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        mb.node().id = "sphere";
        mb.part("sphere", GL20.GL_TRIANGLES,
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
                        new Material(ColorAttribute.createDiffuse(Color.GREEN)))
                .sphere(1f, 1f, 1f, 10, 10);
        mb.node().id = "box";
        mb.part("box", GL20.GL_TRIANGLES,
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
                        new Material(ColorAttribute.createDiffuse(Color.BLUE)))
                .box(size, 20, 1f);
        mb.node().id = "box_min";
        mb.part("box_min", GL20.GL_TRIANGLES,
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
                        new Material(ColorAttribute.createDiffuse(Color.BLUE)))
                .box(minim_wall, 20, 1f);
        mb.node().id = "box_min_2";
        mb.part("box_min_2", GL20.GL_TRIANGLES,
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
                        new Material(ColorAttribute.createDiffuse(Color.BLUE)))
                .box(minim_wall*2, 20, 1f);
        mb.node().id = "box_min_3";
        mb.part("box_min_3", GL20.GL_TRIANGLES,
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
                        new Material(ColorAttribute.createDiffuse(Color.BLUE)))
                .box(minim_wall*3, 20, 1f);
        mb.node().id = "box_min_4";
        mb.part("box_min_4", GL20.GL_TRIANGLES,
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
                        new Material(ColorAttribute.createDiffuse(Color.BLUE)))
                .box(minim_wall*4, 20, 1f);
        mb.node().id = "box_min_5";
        mb.part("box_min_5", GL20.GL_TRIANGLES,
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
                        new Material(ColorAttribute.createDiffuse(Color.BLUE)))
                .box(minim_wall*5, 20, 1f);
        mb.node().id = "box_min_6";
        mb.part("box_min_6", GL20.GL_TRIANGLES,
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
                        new Material(ColorAttribute.createDiffuse(Color.BLUE)))
                .box(minim_wall*6, 20, 1f);
        mb.node().id = "box_min_p";
        mb.part("box_min_p", GL20.GL_TRIANGLES,
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
                        new Material(ColorAttribute.createDiffuse(Color.BLUE)))
                .box((minim_wall*4)+3, 20, 1f);
        model = mb.end();


        ModelLoader loader = new ObjLoader();
        Model modelTest = loader.loadModel(Gdx.files.internal("assets/player.obj"));


        constructors = new ArrayMap<String, GameObject.Constructor>(String.class, GameObject.Constructor.class);
        constructors.put("player", new GameObject.Constructor(modelTest, new btBoxShape(new Vector3(2,10,1))));
        constructors.put("sphere", new GameObject.Constructor(model, "sphere", new btSphereShape(0.5f)));
        //constructors.put("box", new GameObject.Constructor(model, "box", new btBoxShape(new Vector3(5f, 5f, 0.5f))));
        //constructors.put("box02", new GameObject.Constructor(model, "box", new btBoxShape(new Vector3(0.5f, 0.5f, 0.5f))));
        constructors.put("box", new GameObject.Constructor(model, "box", new btBoxShape(new Vector3(size/2, 10f, 0.5f))));
        constructors.put("box_min", new GameObject.Constructor(model, "box_min", new btBoxShape(new Vector3(minim_wall/2, 10f, 0.5f))));
        constructors.put("box_min_2", new GameObject.Constructor(model, "box_min_2", new btBoxShape(new Vector3(minim_wall, 10f, 0.5f))));
        constructors.put("box_min_3", new GameObject.Constructor(model, "box_min_3", new btBoxShape(new Vector3((float) (minim_wall*1.5), 10f, 0.5f))));
        constructors.put("box_min_4", new GameObject.Constructor(model, "box_min_4", new btBoxShape(new Vector3(minim_wall*2, 10f, 0.5f))));
        constructors.put("box_min_5", new GameObject.Constructor(model, "box_min_5", new btBoxShape(new Vector3((float) (minim_wall*2.5), 10f, 0.5f))));
        constructors.put("box_min_6", new GameObject.Constructor(model, "box_min_6", new btBoxShape(new Vector3(minim_wall*3, 10f, 0.5f))));
        constructors.put("box_min_p", new GameObject.Constructor(model, "box_min_p", new btBoxShape(new Vector3(((minim_wall*4)+10)/2, 10f, 0.5f))));
        //Position

        instances = new Array<GameObject>();

        //GameObject object = constructors.get("sphere").construct();
        GameObject object = constructors.get("player").construct();



        collisionConfig = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(collisionConfig);
        broadphase = new btDbvtBroadphase();


        collisionWorld = new btCollisionWorld(dispatcher, broadphase, collisionConfig);

        //makeLaberinto();

        //collisionWorld.addCollisionObject(object.body);

        //CollisionWorld

        //collisionWorld.addCollisionObject(object02.body);
        makeLaberinto();

        object.moving = true;
        object.transform.translate(new Vector3(0,0,-5));
        object.body.setWorldTransform(object.transform);
        object.body.setUserValue(instances.size);
        object.body.setCollisionFlags(object.body.getCollisionFlags() |
                btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);

        System.out.println("SHAPE CIRCULO :" + object.body.getCollisionShape().getName());

        instances.add(object);
        collisionWorld.addCollisionObject(object.body);
    }

    public void makeLaberinto() {
        float a = (float) Math.sqrt(Math.pow(size, 2) - Math.pow(120, 2));
        //Out Walls

        System.out.println("SIZE:" + size);
        GameObject object01_1  = constructors.get("box_min_4").construct();
        GameObject object01_2 = constructors.get("box_min_3").construct();
        GameObject object01 = constructors.get("box").construct();
        GameObject object02 = constructors.get("box").construct();
        GameObject object03 = constructors.get("box").construct();
        GameObject object04 = constructors.get("box").construct();
        GameObject object04_1 = constructors.get("box_min_3").construct();
        GameObject object04_2 = constructors.get("box_min_p").construct();

        GameObject object05 = constructors.get("box").construct();
        GameObject object06 = constructors.get("box").construct();
        GameObject object07 = constructors.get("box_min_4").construct();
        GameObject object08 = constructors.get("box_min_2").construct();
        GameObject object09 = constructors.get("box_min_3").construct();
        GameObject object10 = constructors.get("box_min").construct();
        GameObject object11 = constructors.get("box_min").construct();
        GameObject object12 = constructors.get("box_min_4").construct();
        GameObject object13 = constructors.get("box_min_2").construct();
        GameObject object14 = constructors.get("box_min").construct();
        GameObject object15 = constructors.get("box_min_3").construct();
        GameObject object16 = constructors.get("box_min").construct();
        GameObject object17 = constructors.get("box_min").construct();
        GameObject object18 = constructors.get("box_min").construct();
        GameObject object19 = constructors.get("box_min").construct();
        GameObject object20 = constructors.get("box_min").construct();
        GameObject object21 = constructors.get("box_min_2").construct();
        GameObject object22 = constructors.get("box_min").construct();
        GameObject object23 = constructors.get("box_min").construct();
        GameObject object24 = constructors.get("box_min").construct();
        GameObject object25 = constructors.get("box_min_2").construct();
        GameObject object26 = constructors.get("box_min").construct();
        GameObject object27 = constructors.get("box_min_2").construct();
        GameObject object28 = constructors.get("box_min").construct();
        GameObject object29 = constructors.get("box_min").construct();
        GameObject object30 = constructors.get("box_min").construct();
        GameObject object31 = constructors.get("box_min").construct();
        GameObject object32 = constructors.get("box_min").construct();
        GameObject object33 = constructors.get("box_min").construct();
        GameObject object34 = constructors.get("box_min_2").construct();
        GameObject object35 = constructors.get("box_min").construct();
        GameObject object36 = constructors.get("box_min").construct();
        GameObject object37 = constructors.get("box_min").construct();
        GameObject object38 = constructors.get("box_min").construct();
        GameObject object39 = constructors.get("box_min").construct();
        GameObject object40 = constructors.get("box_min_2").construct();
        GameObject object41 = constructors.get("box_min_2").construct();
        GameObject object42 = constructors.get("box_min").construct();
        GameObject object43 = constructors.get("box_min").construct();
        GameObject object44 = constructors.get("box_min").construct();
        GameObject object45 = constructors.get("box_min").construct();
        GameObject object46 = constructors.get("box_min").construct();
        GameObject object47 = constructors.get("box_min").construct();
        GameObject object48 = constructors.get("box_min").construct();
        GameObject object49 = constructors.get("box_min").construct();
        GameObject object50 = constructors.get("box_min").construct();
        GameObject object51 = constructors.get("box_min").construct();
        GameObject object52 = constructors.get("box_min_2").construct();
        GameObject object53 = constructors.get("box_min").construct();
        GameObject object54 = constructors.get("box_min").construct();
        GameObject object55 = constructors.get("box_min").construct();
        GameObject object56 = constructors.get("box_min").construct();
        GameObject object57 = constructors.get("box_min").construct();
        GameObject object58 = constructors.get("box_min_2").construct();
        GameObject object59 = constructors.get("box_min").construct();
        GameObject object60 = constructors.get("box_min_2").construct();
        GameObject object61 = constructors.get("box_min_3").construct();
        GameObject object62 = constructors.get("box_min").construct();
        GameObject object63 = constructors.get("box_min").construct();
        GameObject object64 = constructors.get("box_min").construct();
        GameObject object65 = constructors.get("box_min_2").construct();
        GameObject object66 = constructors.get("box_min_4").construct();
        GameObject object67 = constructors.get("box_min_2").construct();
        GameObject object68 = constructors.get("box_min_4").construct();
        GameObject object69 = constructors.get("box_min").construct();
        GameObject object70 = constructors.get("box_min_2").construct();
        GameObject object71 = constructors.get("box_min").construct();
        GameObject object72 = constructors.get("box_min").construct();
        GameObject object73 = constructors.get("box_min").construct();
        GameObject object74 = constructors.get("box_min").construct();
        GameObject object75 = constructors.get("box_min").construct();
        GameObject object76 = constructors.get("box_min").construct();
        GameObject object77 = constructors.get("box_min").construct();
        GameObject object78 = constructors.get("box_min_3").construct();
        GameObject object79 = constructors.get("box_min").construct();
        GameObject object80 = constructors.get("box_min").construct();
        GameObject object81 = constructors.get("box_min").construct();
        GameObject object82 = constructors.get("box_min").construct();
        GameObject object83 = constructors.get("box_min").construct();
        GameObject object84 = constructors.get("box_min_2").construct();
        GameObject object85 = constructors.get("box_min_2").construct();
        GameObject object86 = constructors.get("box_min").construct();
        GameObject object87 = constructors.get("box_min_2").construct();
        GameObject object88 = constructors.get("box_min").construct();
        GameObject object89 = constructors.get("box_min").construct();
        GameObject object90 = constructors.get("box_min").construct();
        GameObject object91 = constructors.get("box_min").construct();
        GameObject object92 = constructors.get("box_min").construct();
        GameObject object93 = constructors.get("box_min").construct();
        GameObject object94 = constructors.get("box_min_3").construct();
        GameObject object95 = constructors.get("box_min").construct();
        GameObject object96 = constructors.get("box_min_4").construct();
        GameObject object97 = constructors.get("box_min_3").construct();
        GameObject object98 = constructors.get("box_min").construct();
        GameObject object99 = constructors.get("box_min").construct();
        GameObject object100 = constructors.get("box_min").construct();
        GameObject object101 = constructors.get("box_min").construct();
        GameObject object102 = constructors.get("box_min_3").construct();
        GameObject object103 = constructors.get("box_min").construct();
        GameObject object104 = constructors.get("box_min").construct();
        GameObject object105 = constructors.get("box_min_2").construct();
        GameObject object106 = constructors.get("box_min").construct();
        GameObject object107 = constructors.get("box_min").construct();
        GameObject object108 = constructors.get("box_min").construct();
        GameObject object109 = constructors.get("box_min").construct();
        GameObject object110 = constructors.get("box_min_2").construct();
        GameObject object111 = constructors.get("box_min").construct();
        GameObject object112 = constructors.get("box_min").construct();
        GameObject object113 = constructors.get("box_min").construct();
        GameObject object114 = constructors.get("box_min").construct();
        GameObject object115 = constructors.get("box_min").construct();

        //Entry
        object01_1.transform.trn(36, 0, 120);
        object01_1.body.setWorldTransform(object01_1.transform);
        object01_2.transform.trn(-45 ,0,120);
        object01_2.body.setWorldTransform(object01_2.transform);

        object02.transform.trn((size / 2) + (a / 2), 0, 60);
        object02.transform.rotate(Vector3.Y, 60);
        object02.body.setWorldTransform(object02.transform);
        object03.transform.trn(-((size / 2) + (a / 2)), 0, 60);
        object03.transform.rotate(Vector3.Y, -60);
        object03.body.setWorldTransform(object03.transform);
        //Entry
        object04.transform.trn(0, 0, -120);
        object04.body.setWorldTransform(object04.transform);
        object04_1.transform.trn(43.5f,0,-120);
        object04_1.body.setWorldTransform(object04_1.transform);

        object04_2.transform.trn(-34.5f, 0, -120);
        object04_2.body.setWorldTransform(object04_2.transform);

        object05.transform.trn(-((size / 2) + (a / 2)), 0, -60);
        object05.transform.rotate(Vector3.Y, 60);
        object05.body.setWorldTransform(object05.transform);
        object06.transform.trn(((size / 2) + (a / 2)), 0, -60);
        object06.transform.rotate(Vector3.Y, -60);
        object06.body.setWorldTransform(object06.transform);
        float q = size / 8;
        //Min
        object07.transform.trn((3 * q) + (float) (60 * Math.tan(Math.toRadians(30)) / 2), 0, -90);
        object07.transform.rotate(Vector3.Y, -60);
        object07.body.setWorldTransform(object07.transform);
        object08.transform.trn(q + (float) (30 * Math.tan(Math.toRadians(30)) / 2), 0, -105);
        object08.transform.rotate(Vector3.Y, -60);
        object08.body.setWorldTransform(object08.transform);
        object09.transform.trn((float) (45 * Math.tan(Math.toRadians(30))) / 2, 0, -97.5f);
        object09.transform.rotate(Vector3.Y, -60);
        object09.body.setWorldTransform(object09.transform);
        object10.transform.trn(-q + (float) (15 * Math.tan(Math.toRadians(30))) / 2, 0, -112.5f);
        object10.transform.rotate(Vector3.Y, -60);
        object10.body.setWorldTransform(object10.transform);
        object11.transform.trn(-(2 * q) + (float) (15 * Math.tan(Math.toRadians(30))) / 2, 0, -112.5f);
        object11.transform.rotate(Vector3.Y, -60);
        object11.body.setWorldTransform(object11.transform);
        object12.transform.trn(-(3 * q) - (float) (60 * Math.tan(Math.toRadians(30)) / 2), 0, -90);
        object12.transform.rotate(Vector3.Y, 60);
        object12.body.setWorldTransform(object12.transform);
        object13.transform.trn((2 * q) + (float) (30 * Math.tan(Math.toRadians(30))), 0, -90);
        object13.transform.rotate(Vector3.Y, -60);
        object13.body.setWorldTransform(object13.transform);
        object14.transform.trn((float) (((2 * q) + (float) (30 * Math.tan(Math.toRadians(30)))) - 3.25) - 1, 0, (float) -82.5);
        object14.transform.rotate(Vector3.Y, 60);
        object14.body.setWorldTransform(object14.transform);
        object15.transform.trn((float) (-7.5 + (45 * Math.tan(Math.toRadians(30)))), 0, -75);
        object15.body.setWorldTransform(object15.transform);
        object16.transform.trn((float) ((2 * -q) + (45 * Math.tan(Math.toRadians(30))) - ((15 * Math.tan(Math.toRadians(30))) / 2)) + 2, 0, (float) -82.5);
        object16.transform.rotate(Vector3.Y, -60);
        object16.body.setWorldTransform(object16.transform);
        object17.transform.trn((float) ((2 * -q) + (45 * Math.tan(Math.toRadians(30))) - ((15 * Math.tan(Math.toRadians(30))) / 2)) + 2, 0, (float) -67.5);
        object17.transform.rotate(Vector3.Y, 60);
        object17.body.setWorldTransform(object17.transform);
        object18.transform.trn((float) ((2 * -q) + (45 * Math.tan(Math.toRadians(30))) - ((15 * Math.tan(Math.toRadians(30))) / 2)) + 2, 0, (float) -52.5);
        object18.transform.rotate(Vector3.Y, -60);
        object18.body.setWorldTransform(object18.transform);
        object19.transform.trn((float) ((2 * -q) + (45 * Math.tan(Math.toRadians(30))) - ((15 * Math.tan(Math.toRadians(30))) / 2)) + 2, 0, (float) -37.5);
        object19.transform.rotate(Vector3.Y, 60);
        object19.body.setWorldTransform(object19.transform);
        object20.transform.trn((float) ((2 * -q) + (45 * Math.tan(Math.toRadians(30))) - ((15 * Math.tan(Math.toRadians(30))) / 2)) + 2, 0, (float) -22.5);
        object20.transform.rotate(Vector3.Y, -60);
        object20.body.setWorldTransform(object20.transform);
        object21.transform.trn((float) (+7.5 - (45 * Math.tan(Math.toRadians(30)))), 0, -90);
        object21.body.setWorldTransform(object21.transform);
        object22.transform.trn(-40, 0, -97.5f);
        object22.transform.rotate(Vector3.Y, -60);
        object22.body.setWorldTransform(object22.transform);
        object23.transform.trn(-56, 0, -97.5f);
        object23.transform.rotate(Vector3.Y, -60);
        object23.body.setWorldTransform(object23.transform);
        object24.transform.trn(-77.5f, 0, -60);
        object24.body.setWorldTransform(object24.transform);
        object25.transform.trn(-32f, 0, -60);
        object25.body.setWorldTransform(object25.transform);
        object26.transform.trn(-65f, 0, (float) -67.5);
        object26.transform.rotate(Vector3.Y, 60);
        object26.body.setWorldTransform(object26.transform);
        object27.transform.trn(-45f, 0, -75);
        object27.body.setWorldTransform(object26.transform);
        object28.transform.trn(-65f, 0, (float) -52.5);
        object28.transform.rotate(Vector3.Y, -60);
        object28.body.setWorldTransform(object28.transform);
        object29.transform.trn(-65f, 0, (float) -37.5);
        object29.transform.rotate(Vector3.Y, 60);
        object29.body.setWorldTransform(object29.transform);
        object30.transform.trn(-65f, 0, (float) -22.5);
        object30.transform.rotate(Vector3.Y, -60);
        object30.body.setWorldTransform(object30.transform);
        object31.transform.trn(48, 0, (float) -67.5);
        object31.transform.rotate(Vector3.Y, -60);
        object31.body.setWorldTransform(object31.transform);
        object32.transform.trn(48, 0, (float) -52.5);
        object32.transform.rotate(Vector3.Y, 60);
        object32.body.setWorldTransform(object31.transform);
        object33.transform.trn(61, 0, (float) -60);
        object33.body.setWorldTransform(object31.transform);
        object34.transform.trn(62, 0, (float) -45);
        object34.transform.rotate(Vector3.Y, 60);
        object34.body.setWorldTransform(object34.transform);
        object35.transform.trn(82, 0, (float) -52.5);
        object35.transform.rotate(Vector3.Y, 60);
        object35.body.setWorldTransform(object35.transform);
        object36.transform.trn(82, 0, (float) -37.5);
        object36.transform.rotate(Vector3.Y, -60);
        object36.body.setWorldTransform(object36.transform);
        object37.transform.trn(82, 0, (float) -22.5);
        object37.transform.rotate(Vector3.Y, 60);
        object37.body.setWorldTransform(object37.transform);
        object38.transform.trn(67, 0, (float) -37.5);
        object38.transform.rotate(Vector3.Y, -60);
        object38.body.setWorldTransform(object38.transform);
        object39.transform.trn(45, 0, (float) -30);
        object39.body.setWorldTransform(object39.transform);
        object40.transform.trn(28, 0, (float) -45);
        object40.transform.rotate(Vector3.Y, -60);
        object40.body.setWorldTransform(object40.transform);
        object41.transform.trn(20, 0, (float) -60);
        object41.body.setWorldTransform(object41.transform);
        object42.transform.trn(32, 0, (float) -22.5);
        object42.transform.rotate(Vector3.Y, 60);
        object42.body.setWorldTransform(object42.transform);
        object43.transform.trn(2, 0, (float) -45);
        object43.body.setWorldTransform(object43.transform);
        object44.transform.trn(2, 0, (float) -15);
        object44.body.setWorldTransform(object44.transform);
        object45.transform.trn(2, 0, (float) 15);
        object45.body.setWorldTransform(object45.transform);
        object46.transform.trn(23, 0, (float) -22.5);
        object46.transform.rotate(Vector3.Y, -60);
        object46.body.setWorldTransform(object46.transform);
        object47.transform.trn(10, 0, (float) -30);
        object47.body.setWorldTransform(object47.transform);
        object48.transform.trn(-52.5f, 0, (float) -45);
        object48.body.setWorldTransform(object48.transform);
        object49.transform.trn(-39.5f, 0, (float) -37.5);
        object49.transform.rotate(Vector3.Y, -60);
        object49.body.setWorldTransform(object49.transform);
        object50.transform.trn(-30.5f, 0, (float) -37.5);
        object50.transform.rotate(Vector3.Y, 60);
        object50.body.setWorldTransform(object50.transform);
        object51.transform.trn(122, 0, (float) -15);
        object51.body.setWorldTransform(object51.transform);
        object52.transform.trn(105.5f, 0, (float) -30);
        object52.transform.rotate(Vector3.Y, -60);
        object52.body.setWorldTransform(object52.transform);
        object53.transform.trn(-120, 0, (float) -15);
        object53.body.setWorldTransform(object53.transform);
        object54.transform.trn(-112.5f, 0, (float) -30);
        object54.body.setWorldTransform(object54.transform);
        object55.transform.trn(-99.5f, 0, (float) -22.5);
        object55.transform.rotate(Vector3.Y, -60);
        object55.body.setWorldTransform(object55.transform);
        object56.transform.trn(-99.5f, 0, -37.5f);
        object56.transform.rotate(Vector3.Y, 60);
        object56.body.setWorldTransform(object56.transform);
        object57.transform.trn(-87f, 0, (float) -45);
        object57.body.setWorldTransform(object57.transform);
        object58.transform.trn(61f, 0, (float) -15);
        object58.body.setWorldTransform(object58.transform);
        object59.transform.trn(57f, 0, -7.5f);
        object59.transform.rotate(Vector3.Y, 60);
        object59.body.setWorldTransform(object59.transform);
        object60.transform.trn(36f, 0, 0);
        object60.transform.rotate(Vector3.Y, 60);
        object60.body.setWorldTransform(object60.transform);
        object61.transform.trn(9.5f, 0, 0);
        object61.body.setWorldTransform(object61.transform);
        object62.transform.trn(-19.5f, 0, -7.5f);
        object62.transform.rotate(Vector3.Y, -60);
        object62.body.setWorldTransform(object62.transform);
        object63.transform.trn(-32f,0, -15f);
        object63.body.setWorldTransform(object63.transform);
        object64.transform.trn(-44f, 0, -22.5f);
        object64.transform.rotate(Vector3.Y, -60);
        object64.body.setWorldTransform(object64.transform);
        object65.transform.trn(-48f, 0, 0);
        object65.transform.rotate(Vector3.Y, 60);
        object65.body.setWorldTransform(object65.transform);
        object66.transform.trn(-81f,0, 0);
        object66.body.setWorldTransform(object66.transform);
        object67.transform.trn(-81f, 0, -15f);
        object67.transform.rotate(Vector3.Y, -60);
        object67.body.setWorldTransform(object67.transform);
        object68.transform.trn(-32f, 0, 30);
        object68.transform.rotate(Vector3.Y, 60);
        object68.body.setWorldTransform(object68.transform);
        object69.transform.trn(-31.5f, 0, 45);
        object69.body.setWorldTransform(object69.transform);
        object70.transform.trn(-15f, 0, 30);
        object70.transform.rotate(Vector3.Y, 60);
        object70.body.setWorldTransform(object70.transform);
        object71.transform.trn(-120, 0, (float) 15);
        object71.body.setWorldTransform(object71.transform);
        object72.transform.trn(-100, 0, 52.5f);
        object72.transform.rotate(Vector3.Y, 60);
        object72.body.setWorldTransform(object72.transform);
        object73.transform.trn(-100, 0, 37.5f);
        object73.transform.rotate(Vector3.Y, -60);
        object73.body.setWorldTransform(object73.transform);
        object74.transform.trn(-100, 0, 22.5f);
        object74.transform.rotate(Vector3.Y, 60);
        object74.body.setWorldTransform(object74.transform);
        object75.transform.trn(-88f, 0, 15);
        object75.body.setWorldTransform(object75.transform);
        object76.transform.trn(-75, 0, 22.5f);
        object76.transform.rotate(Vector3.Y, -60);
        object76.body.setWorldTransform(object76.transform);
        object77.transform.trn(-62, 0, 30f);
        object77.body.setWorldTransform(object77.transform);
        object78.transform.trn(-53, 0, 30);
        object78.transform.rotate(Vector3.Y, 60);
        object78.body.setWorldTransform(object78.transform);
        object79.transform.trn(-70, 0, 45);
        object79.body.setWorldTransform(object79.transform);
        object80.transform.trn(-18.5f, 0, 52.5f);
        object80.transform.rotate(Vector3.Y, -60);
        object80.body.setWorldTransform(object78.transform);
        object81.transform.trn(-18.5f, 0, 67.5f);
        object81.transform.rotate(Vector3.Y, 60);
        object81.body.setWorldTransform(object81.transform);
        object82.transform.trn(-18.5f, 0, 82.5f);
        object82.transform.rotate(Vector3.Y, -60);
        object82.body.setWorldTransform(object82.transform);
        object83.transform.trn(-6.5f, 0, 60f);
        object83.body.setWorldTransform(object83.transform);
        object84.transform.trn(2f, 0, 60);
        object84.transform.rotate(Vector3.Y, -60);
        object84.body.setWorldTransform(object84.transform);
        object85.transform.trn(-67, 0, 90);
        object85.transform.rotate(Vector3.Y, 60);
        object85.body.setWorldTransform(object85.transform);
        object86.transform.trn(-50, 0, 75);
        object86.body.setWorldTransform(object86.transform);
        object87.transform.trn(-33, 0, 89.5f);
        object87.transform.rotate(Vector3.Y, -60);
        object87.body.setWorldTransform(object87.transform);
        object88.transform.trn(-16.5f, 0, 105);
        object88.body.setWorldTransform(object88.transform);
        object89.transform.trn(-3.5f, 0, 98.5f);
        object89.transform.rotate(Vector3.Y, 60);
        object89.body.setWorldTransform(object89.transform);
        object90.transform.trn(-3.5f, 0, 83.5f);
        object90.transform.rotate(Vector3.Y, -60);
        object90.body.setWorldTransform(object90.transform);
        object91.transform.trn(9.5f, 0, 90);
        object91.body.setWorldTransform(object91.transform);
        object92.transform.trn(21.5f, 0, 82.5f);
        object92.transform.rotate(Vector3.Y, 60);
        object92.body.setWorldTransform(object92.transform);
        object93.transform.trn(33.5f, 0, 75);
        object93.body.setWorldTransform(object93.transform);
        object94.transform.trn(12.5f, 0, 52.5f);
        object94.transform.rotate(Vector3.Y, -60);
        object94.body.setWorldTransform(object94.transform);
        object95.transform.trn(37.5f, 0, 67.5f);
        object95.transform.rotate(Vector3.Y, -60);
        object95.body.setWorldTransform(object95.transform);
        object96.transform.trn(33f, 0, 30);
        object96.body.setWorldTransform(object96.transform);
        object97.transform.trn(89.5f, 0, 7.5f);
        object97.transform.rotate(Vector3.Y, -60);
        object97.body.setWorldTransform(object97.transform);
        object98.transform.trn(76.5f, 0, 0);
        object98.body.setWorldTransform(object98.transform);
        object99.transform.trn(64.5f, 0, 7.5f);
        object99.transform.rotate(Vector3.Y, 60);
        object99.body.setWorldTransform(object99.transform);
        object100.transform.trn(71f, 0, 22.5f);
        object100.transform.rotate(Vector3.Y, 60);
        object100.body.setWorldTransform(object100.transform);
        object101.transform.trn(51.5f, 0, 15f);
        object101.body.setWorldTransform(object101.transform);
        object102.transform.trn(91f, 0, 22.5f);
        object102.transform.rotate(Vector3.Y, 60);
        object102.body.setWorldTransform(object102.transform);
        object103.transform.trn(98.5f, 0, 36.5f);
        object103.transform.rotate(Vector3.Y, 60);
        object103.body.setWorldTransform(object103.transform);
        object104.transform.trn(21f, 0, 52.5f);
        object104.transform.rotate(Vector3.Y, 60);
        object104.body.setWorldTransform(object104.transform);
        object105.transform.trn(41.5f, 0, 45f);
        object105.body.setWorldTransform(object105.transform);
        object106.transform.trn(63f, 0, 52.5f);
        object106.transform.rotate(Vector3.Y, -60);
        object106.body.setWorldTransform(object106.transform);
        object107.transform.trn(76f, 0, 60f);
        object107.body.setWorldTransform(object107.transform);
        object108.transform.trn(80f, 0, 67.5f);
        object108.transform.rotate(Vector3.Y, 60);
        object108.body.setWorldTransform(object108.transform);
        object109.transform.trn(78f, 0, 90f);
        object109.body.setWorldTransform(object109.transform);
        object110.transform.trn(61f, 0, 75f);
        object110.transform.rotate(Vector3.Y, -60);
        object110.body.setWorldTransform(object110.transform);
        object111.transform.trn(65f, 0, 97.5f);
        object111.transform.rotate(Vector3.Y, 60);
        object111.body.setWorldTransform(object111.transform);
        object112.transform.trn(52f, 0, 105f);
        object112.body.setWorldTransform(object112.transform);
        object113.transform.trn(30f, 0, 112.5f);
        object113.transform.rotate(Vector3.Y, -60);
        object113.body.setWorldTransform(object113.transform);
        object114.transform.trn(30f, 0, 97.5f);
        object114.transform.rotate(Vector3.Y, 60);
        object114.body.setWorldTransform(object114.transform);
        object115.transform.trn(17f, 0, 105f);
        object115.body.setWorldTransform(object115.transform);
        //object01.body.setUserValue(instances.size);

        instances.add(object01_1);
        instances.add(object01_2);
        instances.add(object02);
        instances.add(object03);
        //instances.add(object04);
        instances.add(object04_1);
        instances.add(object04_2);
        instances.add(object05);
        instances.add(object06);
        instances.add(object07);
        instances.add(object08);
        instances.add(object09);
        instances.add(object10);
        instances.add(object11);
        instances.add(object12);
        instances.add(object13);
        instances.add(object14);
        instances.add(object15);
        instances.add(object16);
        instances.add(object17);
        instances.add(object18);
        instances.add(object19);
        instances.add(object20);
        instances.add(object21);
        instances.add(object22);
        instances.add(object23);
        instances.add(object24);
        instances.add(object25);
        instances.add(object26);
        instances.add(object27);
        instances.add(object28);
        instances.add(object29);
        instances.add(object30);
        instances.add(object31);
        instances.add(object32);
        instances.add(object33);
        instances.add(object34);
        instances.add(object35);
        instances.add(object36);
        instances.add(object37);
        instances.add(object38);
        instances.add(object39);
        instances.add(object40);
        instances.add(object41);
        instances.add(object42);
        instances.add(object43);
        instances.add(object44);
        instances.add(object45);
        instances.add(object46);
        instances.add(object47);
        instances.add(object48);
        instances.add(object49);
        instances.add(object50);
        instances.add(object51);
        instances.add(object52);
        instances.add(object53);
        instances.add(object54);
        instances.add(object55);
        instances.add(object56);
        instances.add(object57);
        instances.add(object58);
        instances.add(object59);
        instances.add(object60);
        instances.add(object61);
        instances.add(object62);
        instances.add(object63);
        instances.add(object64);
        instances.add(object65);
        instances.add(object66);
        instances.add(object67);
        instances.add(object68);
        instances.add(object69);
        instances.add(object70);
        instances.add(object71);
        instances.add(object72);
        instances.add(object73);
        instances.add(object74);
        instances.add(object75);
        instances.add(object76);
        instances.add(object77);
        instances.add(object78);
        instances.add(object79);
        instances.add(object80);
        instances.add(object81);
        instances.add(object82);
        instances.add(object83);
        instances.add(object84);
        instances.add(object85);
        instances.add(object86);
        instances.add(object87);
        instances.add(object88);
        instances.add(object89);
        instances.add(object90);
        instances.add(object91);
        instances.add(object92);
        instances.add(object93);
        instances.add(object94);
        instances.add(object95);
        instances.add(object96);
        instances.add(object97);
        instances.add(object98);
        instances.add(object99);
        instances.add(object100);
        instances.add(object101);
        instances.add(object102);
        instances.add(object103);
        instances.add(object104);
        instances.add(object105);
        instances.add(object106);
        instances.add(object107);
        instances.add(object108);
        instances.add(object109);
        instances.add(object110);
        instances.add(object111);
        instances.add(object112);
        instances.add(object113);
        instances.add(object114);
        instances.add(object115);

        collisionWorld.addCollisionObject(object01_1.body);
        collisionWorld.addCollisionObject(object01_2.body);
        collisionWorld.addCollisionObject(object02.body);
        collisionWorld.addCollisionObject(object03.body);
        //collisionWorld.addCollisionObject(object04.body);
        collisionWorld.addCollisionObject(object04_1.body);
        collisionWorld.addCollisionObject(object04_2.body);
        collisionWorld.addCollisionObject(object05.body);
        collisionWorld.addCollisionObject(object06.body);
        collisionWorld.addCollisionObject(object07.body);
        collisionWorld.addCollisionObject(object08.body);
        collisionWorld.addCollisionObject(object09.body);
        collisionWorld.addCollisionObject(object10.body);
        collisionWorld.addCollisionObject(object11.body);
        collisionWorld.addCollisionObject(object12.body);
        collisionWorld.addCollisionObject(object13.body);
        collisionWorld.addCollisionObject(object14.body);
        collisionWorld.addCollisionObject(object15.body);
        collisionWorld.addCollisionObject(object16.body);
        collisionWorld.addCollisionObject(object17.body);
        collisionWorld.addCollisionObject(object18.body);
        collisionWorld.addCollisionObject(object19.body);
        collisionWorld.addCollisionObject(object20.body);
        collisionWorld.addCollisionObject(object21.body);
        collisionWorld.addCollisionObject(object22.body);
        collisionWorld.addCollisionObject(object23.body);
        collisionWorld.addCollisionObject(object24.body);
        collisionWorld.addCollisionObject(object25.body);
        collisionWorld.addCollisionObject(object26.body);
        collisionWorld.addCollisionObject(object27.body);
        collisionWorld.addCollisionObject(object28.body);
        collisionWorld.addCollisionObject(object29.body);
        collisionWorld.addCollisionObject(object30.body);
        collisionWorld.addCollisionObject(object31.body);
        collisionWorld.addCollisionObject(object32.body);
        collisionWorld.addCollisionObject(object33.body);
        collisionWorld.addCollisionObject(object34.body);
        collisionWorld.addCollisionObject(object35.body);
        collisionWorld.addCollisionObject(object36.body);
        collisionWorld.addCollisionObject(object37.body);
        collisionWorld.addCollisionObject(object38.body);
        collisionWorld.addCollisionObject(object39.body);
        collisionWorld.addCollisionObject(object40.body);
        collisionWorld.addCollisionObject(object41.body);
        collisionWorld.addCollisionObject(object42.body);
        collisionWorld.addCollisionObject(object43.body);
        collisionWorld.addCollisionObject(object44.body);
        collisionWorld.addCollisionObject(object45.body);
        collisionWorld.addCollisionObject(object46.body);
        collisionWorld.addCollisionObject(object47.body);
        collisionWorld.addCollisionObject(object48.body);
        collisionWorld.addCollisionObject(object49.body);
        collisionWorld.addCollisionObject(object50.body);
        collisionWorld.addCollisionObject(object51.body);
        collisionWorld.addCollisionObject(object52.body);
        collisionWorld.addCollisionObject(object53.body);
        collisionWorld.addCollisionObject(object54.body);
        collisionWorld.addCollisionObject(object55.body);
        collisionWorld.addCollisionObject(object56.body);
        collisionWorld.addCollisionObject(object57.body);
        collisionWorld.addCollisionObject(object58.body);
        collisionWorld.addCollisionObject(object59.body);
        collisionWorld.addCollisionObject(object60.body);
        collisionWorld.addCollisionObject(object61.body);
        collisionWorld.addCollisionObject(object62.body);
        collisionWorld.addCollisionObject(object63.body);
        collisionWorld.addCollisionObject(object64.body);
        collisionWorld.addCollisionObject(object65.body);
        collisionWorld.addCollisionObject(object66.body);
        collisionWorld.addCollisionObject(object67.body);
        collisionWorld.addCollisionObject(object68.body);
        collisionWorld.addCollisionObject(object69.body);
        collisionWorld.addCollisionObject(object70.body);
        collisionWorld.addCollisionObject(object71.body);
        collisionWorld.addCollisionObject(object72.body);
        collisionWorld.addCollisionObject(object73.body);
        collisionWorld.addCollisionObject(object74.body);
        collisionWorld.addCollisionObject(object75.body);
        collisionWorld.addCollisionObject(object76.body);
        collisionWorld.addCollisionObject(object77.body);
        collisionWorld.addCollisionObject(object78.body);
        collisionWorld.addCollisionObject(object79.body);
        collisionWorld.addCollisionObject(object80.body);
        collisionWorld.addCollisionObject(object81.body);
        collisionWorld.addCollisionObject(object82.body);
        collisionWorld.addCollisionObject(object83.body);
        collisionWorld.addCollisionObject(object84.body);
        collisionWorld.addCollisionObject(object85.body);
        collisionWorld.addCollisionObject(object86.body);
        collisionWorld.addCollisionObject(object87.body);
        collisionWorld.addCollisionObject(object88.body);
        collisionWorld.addCollisionObject(object89.body);
        collisionWorld.addCollisionObject(object90.body);
        collisionWorld.addCollisionObject(object91.body);
        collisionWorld.addCollisionObject(object92.body);
        collisionWorld.addCollisionObject(object93.body);
        collisionWorld.addCollisionObject(object94.body);
        collisionWorld.addCollisionObject(object95.body);
        collisionWorld.addCollisionObject(object96.body);
        collisionWorld.addCollisionObject(object97.body);
        collisionWorld.addCollisionObject(object98.body);
        collisionWorld.addCollisionObject(object99.body);
        collisionWorld.addCollisionObject(object100.body);
        collisionWorld.addCollisionObject(object101.body);
        collisionWorld.addCollisionObject(object102.body);
        collisionWorld.addCollisionObject(object103.body);
        collisionWorld.addCollisionObject(object104.body);
        collisionWorld.addCollisionObject(object105.body);
        collisionWorld.addCollisionObject(object106.body);
        collisionWorld.addCollisionObject(object107.body);
        collisionWorld.addCollisionObject(object108.body);
        collisionWorld.addCollisionObject(object109.body);
        collisionWorld.addCollisionObject(object110.body);
        collisionWorld.addCollisionObject(object111.body);
        collisionWorld.addCollisionObject(object112.body);
        collisionWorld.addCollisionObject(object113.body);
        collisionWorld.addCollisionObject(object114.body);
        collisionWorld.addCollisionObject(object115.body);



    }

    @Override
    public void render() {
        final float delta = Math.min(1f / 30f, Gdx.graphics.getDeltaTime());
        GameObject obj = instances.get(instances.size-1);

        //Mover
        /*
       for (int i = 1; i < 10; i++) {
            if(obj.moving){
                obj.transform.trn(-delta, 0f, 0f);
                obj.body.setWorldTransform(obj.transform);
            }

        }

         */

        collisionWorld.performDiscreteCollisionDetection();

        processInput(delta);
        updateCamera();
        //camController.update();

        Gdx.gl.glClearColor(0.3f, 0.3f, 0.3f, 1.f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(cam);
        modelBatch.render(instances, environment);
        modelBatch.end();
    }


    @Override
    public void dispose() {

        for (GameObject obj: instances) obj.dispose();
        instances.clear();

        for (GameObject.Constructor ctor : constructors.values()) ctor.dispose();
        constructors.clear();

        groundObject.dispose();
        groundShape.dispose();

        ballObject.dispose();
        ballShape.dispose();

        dispatcher.dispose();
        collisionConfig.dispose();

        modelBatch.dispose();
        model.dispose();
        contactListener.dispose();
        collisionWorld.dispose();
        broadphase.dispose();
    }

    public void processInput(float deltaTime){
        playerTransform.set(instances.get(instances.size-1).transform);
        GameObject player = instances.get(instances.size-1);
        if(Gdx.input.isKeyPressed(Input.Keys.ESCAPE)){
            Gdx.app.exit();

        }
        if(Gdx.input.isKeyPressed(Input.Keys.W)){
            if(!moveDown && !player.moving){
                moveDown = true;
                player.moving  = true;
            }
            if(!player.moving) moveUp = false;
            if(moveUp){
                moveTranslation.z += speed * deltaTime;
            }

        }
        if(Gdx.input.isKeyPressed(Input.Keys.S)){
            if(!moveUp && !player.moving){
                moveUp = true;
                player.moving  = true;
            }
            if(!player.moving) moveDown = false;
            if(moveDown){
                moveTranslation.z -= speed * deltaTime;
            }
        }

        if(Gdx.input.isKeyPressed(Input.Keys.A)){
            playerTransform.rotate(Vector3.Y, rotationspeed * deltaTime);
            angleBehindPlayer += rotationspeed * deltaTime;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.D)){
            playerTransform.rotate(Vector3.Y, -rotationspeed * deltaTime);
            angleBehindPlayer -= rotationspeed * deltaTime;
        }

        if(Gdx.input.isKeyPressed(Input.Keys.TAB)){
            switch (cameraMode){
                case FREE_LOOK:
                    cameraMode = CameraMode.BEHIND_PLAYER;
                    angleAroundPlayer = angleBehindPlayer;
                    break;
                case BEHIND_PLAYER:
                    cameraMode = CameraMode.FREE_LOOK;
                    break;
            }
        }

        playerTransform.translate(moveTranslation);
        player.transform.set(playerTransform);
        player.transform.getTranslation(currentPosition);
        player.body.setWorldTransform(player.transform);
        moveTranslation.set(0,0,0);
    }

    private void updateCamera(){
        float horDistance = calculateHorizontalDistance(distanceFromPlayer);
        float verDistance = calculateVerticalDistance(distanceFromPlayer);
        calculatePitch();
        calculateAngleAroundPlayer();
        calculateCameraPosition(currentPosition, horDistance, verDistance);

        cam.up.set(Vector3.Y);
        cam.lookAt(currentPosition);
        cam.update();
    }

    private void calculateCameraPosition(Vector3 currentPosition, float horDistance, float verDistance) {
        float offsetX = (float) (horDistance * Math.sin(Math.toRadians(angleAroundPlayer)));
        float offsetZ = (float) (horDistance * Math.cos(Math.toRadians(angleAroundPlayer)));

        cam.position.x = currentPosition.x - offsetX;
        cam.position.z = currentPosition.z - offsetZ;
        cam.position.y = currentPosition.y + verDistance;

    }
    private void calculateAngleAroundPlayer() {
        if(cameraMode == CameraMode.FREE_LOOK){
            float angleChange = Gdx.input.getDeltaX() * Settings.CAMERA_ANGLE_AROUND_PLAYER_FACTOR;
            angleAroundPlayer -= angleChange;
        }else if(cameraMode == CameraMode.BEHIND_PLAYER){
            angleAroundPlayer = angleBehindPlayer;
        }
    }

    private void calculatePitch() {
        float pitchChange = -Gdx.input.getDeltaY() * Settings.CAMERA_PITCH_FACTOR;
        camPitch -= pitchChange;

        if(camPitch < Settings.CAMERA_MIN_PITCH){
            camPitch  = Settings.CAMERA_MIN_PITCH;
        }else if(camPitch > Settings.CAMERA_MAX_PITCH){
            camPitch = Settings.CAMERA_MAX_PITCH;
        }
    }

    private float calculateHorizontalDistance(float distanceFromPlayer) {
        return (float) (distanceFromPlayer * Math.sin(Math.toRadians(camPitch)));
    }

    private float calculateVerticalDistance(float distanceFromPlayer) {
        return (float) (distanceFromPlayer * Math.cos(Math.toRadians(camPitch)));
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        float zoomLevel = amountY * Settings.CAMERA_ZOOM_LEVEL_FACTOR;
        distanceFromPlayer += zoomLevel;
        if(distanceFromPlayer < Settings.CAMERA_MIN_DISTANCE_FROM_PLAYER)
            distanceFromPlayer = Settings.CAMERA_MIN_DISTANCE_FROM_PLAYER;

        return false;
    }

}
