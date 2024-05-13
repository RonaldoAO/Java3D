package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;


import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.enums.CameraMode;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;

import java.util.ArrayList;
import java.util.List;

public class CollisionsT1 extends ApplicationAdapter implements InputProcessor
{

    private SceneManager sceneManager;
    private SceneAsset sceneAsset;
    private Scene playerScene;
    private PerspectiveCamera camera;
    private Cubemap diffuseCubemap;
    private Cubemap environmentCubemap;
    private Cubemap specularCubemap;
    private Texture brdfLUT;
    private float time;
    private SceneSkybox skybox;
    private DirectionalLightEx light;
//	private FirstPersonCameraController cameraController;

    //Player Movement
    float speed = 5f;
    float rotationspeed = 80f;
    private Matrix4 playerTransform = new Matrix4();
    private final Vector3 moveTranslation = new Vector3();
    private final Vector3 currentPosition = new Vector3();

    //Camera
    private CameraMode cameraMode = CameraMode.BEHIND_PLAYER;
    private float camPitch = Settings.CAMERA_START_PITCH;
    private float distanceFromPlayer = 35f;
    private float angleAroundPlayer  = 0f;
    private float angleBehindPlayer = 0f;

    boolean collision;
    btCollisionShape groundShape;
    btCollisionShape ballShape;
    btCollisionObject groundObject;
    List<btCollisionObject> groundObjects = new ArrayList<>();
    btCollisionObject ballObject;

    btCollisionConfiguration collisionConfig;
    btDispatcher dispatcher;

    Array<ModelInstance> instances;
    ModelBatch modelBatch;
    Environment environment;

    @Override
    public void create() {
        modelBatch = new ModelBatch();
        Bullet.init();



        // create scene
        sceneAsset = new GLTFLoader().load(Gdx.files.internal("models/untitled.gltf"));

        playerScene = new Scene(sceneAsset.scene);
        playerScene.modelInstance.transform.scl(0.5f);
        playerScene.modelInstance.transform.translate(0,0,-20f);
        //sceneManager = new SceneManager();
        //sceneManager.addScene(playerScene);


        // setup camera (The BoomBox model is very small so you may need to adapt camera settings for your scene)
        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 1f;
        camera.far = 200;
        //sceneManager.setCamera(camera);
        camera.position.set(0, 0, 4f);
        camera.update();

        Gdx.input.setCursorCatched(true);
        Gdx.input.setInputProcessor(this);

        //cameraController = new FirstPersonCameraController(camera);
        //Gdx.input.setInputProcessor(cameraController);

        // setup light
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
       // sceneManager.environment.add(light);

        // setup quick IBL (image based lighting)


        // This texture is provided by the library, no need to have it in your assets.
        brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));


        // setup skybox
        //skybox = new SceneSkybox(environmentCubemap);
        //sceneManager.setSkyBox(skybox);

        instances = new Array<ModelInstance>();

        ballShape = new btSphereShape(0.25f);
        groundShape = new btBoxShape(new Vector3(0.5f, 1f, 0.5f));

        //buildBoxes();

        ballObject = new btCollisionObject();
        ballObject.setCollisionShape(ballShape);
        ballObject.setWorldTransform(playerScene.modelInstance.transform);

        collisionConfig = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(collisionConfig);

        instances.add(playerScene.modelInstance);
    }

    private void buildBoxes() {
        ModelBuilder modelBuilder = new ModelBuilder();

        for (int i = 0; i < 100; i+= 10) {
            for (int j = 0; j < 100; j+= 10) {
                modelBuilder.begin();
                Material material = new Material();
                material.set(PBRColorAttribute.createBaseColorFactor(Color.RED));
                MeshPartBuilder builder = modelBuilder.part(i + ", " + j, GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, material);
                BoxShapeBuilder.build(builder, i, 0f, j, 1f, 2f, 1f);
                ModelInstance model  = new ModelInstance(modelBuilder.end());
                groundObject = new btCollisionObject();
                groundObject.setCollisionShape(groundShape);
                groundObject.setWorldTransform(model.transform);
                groundObjects.add(groundObject);
                instances.add(model);
            }
        }


        //Object


        //sceneManager.addScene(new Scene(model));
    }

    @Override
    public void resize(int width, int height) {
        //sceneManager.updateViewport(width, height);
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        time += deltaTime;

        //cameraController.update();
        if(!collision){
            processInput(deltaTime);
            collision = checkCollision();
        }

        updateCamera();

        //Rotate the model
        //playerScene.modelInstance.transform.rotate(Vector3.Y, 10f * deltaTime);

        // render
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        //sceneManager.update(deltaTime);
        //sceneManager.render();
        modelBatch.begin(camera);
        modelBatch.render(instances, environment);
        modelBatch.end();
    }

    private boolean checkCollision() {
        CollisionObjectWrapper co0 = new CollisionObjectWrapper(ballObject);



        CollisionObjectWrapper co1 = new CollisionObjectWrapper(groundObject);

        btCollisionAlgorithmConstructionInfo ci = new btCollisionAlgorithmConstructionInfo();
        ci.setDispatcher1(dispatcher);
        btCollisionAlgorithm algorithm = new btSphereBoxCollisionAlgorithm(null, ci,co0.wrapper, co1.wrapper, false);

        btDispatcherInfo info = new btDispatcherInfo();
        btManifoldResult result = new btManifoldResult(co0.wrapper, co1.wrapper);

        algorithm.processCollision(co0.wrapper, co1.wrapper, info, result);

        boolean r = result.getPersistentManifold().getNumContacts() > 0;
        result.dispose();
        info.dispose();
        algorithm.dispose();
        ci.dispose();
        co0.dispose();
        co1.dispose();

        return r;
    }

    private void updateCamera() {

        float horDistance = calculateHorizontalDistance(distanceFromPlayer);
        float verDistance = calculateVerticalDistance(distanceFromPlayer);

        calculatePitch();
        calculateAngleAroundPlayer();
        calculateCameraPosition(currentPosition, horDistance, verDistance);

        camera.up.set(Vector3.Y);
        camera.lookAt(currentPosition);
        camera.update();
    }

    private void calculateCameraPosition(Vector3 currentPosition, float horDistance, float verDistance) {
        float offsetX = (float) (horDistance * Math.sin(Math.toRadians(angleAroundPlayer)));
        float offsetZ = (float) (horDistance * Math.cos(Math.toRadians(angleAroundPlayer)));

        camera.position.x = currentPosition.x - offsetX;
        camera.position.z = currentPosition.z - offsetZ;
        camera.position.y = currentPosition.y + verDistance;

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

    private float calculateVerticalDistance(float distanceFromPlayer) {
        return (float) (distanceFromPlayer * Math.sin(Math.toRadians(camPitch)));
    }

    private float calculateHorizontalDistance(float distanceFromPlayer) {
        return (float) (distanceFromPlayer * Math.cos(Math.toRadians(camPitch)));
    }

    private void processInput(float deltaTime) {
        //update the player transform
        playerTransform.set(playerScene.modelInstance.transform);
        if(Gdx.input.isKeyPressed(Input.Keys.ESCAPE)){
            Gdx.app.exit();
        }

        if(Gdx.input.isKeyPressed(Input.Keys.W)){
            moveTranslation.z += speed * deltaTime;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.S)){
            moveTranslation.z -= speed * deltaTime;
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


        //Apply the move translation to the transform
        playerTransform.translate(moveTranslation);

        // Set the modified transform
        playerScene.modelInstance.transform.set(playerTransform);

        //update vector position
        playerScene.modelInstance.transform.getTranslation(currentPosition);

        //clear the move translation out
        moveTranslation.set(0,0,0);
    }

    @Override
    public void dispose() {
        groundObject.dispose();
        groundShape.dispose();
        ballObject.dispose();
        ballShape.dispose();
        dispatcher.dispose();
        collisionConfig.dispose();
        sceneManager.dispose();
        sceneAsset.dispose();
        environmentCubemap.dispose();
        diffuseCubemap.dispose();
        specularCubemap.dispose();
        brdfLUT.dispose();
        skybox.dispose();
        modelBatch.dispose();
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