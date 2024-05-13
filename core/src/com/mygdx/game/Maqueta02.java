package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;


public class Maqueta02 extends ApplicationAdapter
{
    private SceneManager sceneManager;
    private SceneAsset sceneAsset;
    private Scene scene;
    private PerspectiveCamera camera;
    private Cubemap diffuseCubemap;
    private Cubemap environmentCubemap;
    private Cubemap specularCubemap;
    private Texture brdfLUT;
    private float time;
    private SceneSkybox skybox;
    private DirectionalLightEx light;
    CameraInputController camController;

    @Override
    public void create() {

        // create scene
        sceneAsset = new GLTFLoader().load(Gdx.files.internal("maqueta/scene.gltf"));
        scene = new Scene(sceneAsset.scene);
        sceneManager = new SceneManager();
        sceneManager.addScene(scene);

        // setup camera (The BoomBox model is very small so you may need to adapt camera settings for your scene)
        camera = new PerspectiveCamera(67f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        float d = .02f;
        //camera.near = d / 1000f;
        camera.far = 1000;
        camera.position.set(106.164246f,21.301752f,-60.25742f);
        camera.direction.set(-0.97139394f,-0.23743708f,0.004101047f);

        //camera.lookAt(0, 0, -80 );
        camera.update();
        camController = new CameraInputController(camera);
        Gdx.input.setInputProcessor(camController);


        sceneManager.setCamera(camera);

        // setup light
        light = new DirectionalLightEx();
        light.direction.set(-0.027865367f,-0.999611f,0.0012127052f).nor();
        light.color.set(Color.WHITE);
        sceneManager.environment.add(light);

        // setup quick IBL (image based lighting)
        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(light);
        environmentCubemap = iblBuilder.buildEnvMap(1024);
        diffuseCubemap = iblBuilder.buildIrradianceMap(256);
        specularCubemap = iblBuilder.buildRadianceMap(10);
        iblBuilder.dispose();

        // This texture is provided by the library, no need to have it in your assets.
        brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));

        sceneManager.setAmbientLight(1f);
        sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap));
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap));

        // setup skybox
        skybox = new SceneSkybox(environmentCubemap);
        sceneManager.setSkyBox(skybox);
    }

    @Override
    public void resize(int width, int height) {
        sceneManager.updateViewport(width, height);
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        time += deltaTime;


        // animate camera
        camController.update();
        inputProcesor();

        // render
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        sceneManager.update(deltaTime);
        sceneManager.render();
    }

    public void inputProcesor(){
        if(Gdx.input.isKeyPressed(Input.Keys.P)){
            System.out.println("POS "+ camera.position);
            System.out.println("DIR "+ camera.direction);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.NUM_1)){
            camera.position.set(54.330853f,4.1715636f,-80.30019f);
            camera.direction.set(-0.0565899f,0.2124403f,0.97553325f);
            //camera.lookAt(0, 0, 0);
            camera.up.set(Vector3.Y); // Asegurar que el vector "up" de la cámara sea el eje Y
            camera.update();

            // Restablecer la matriz de transformación de la cámara
            camera.view.setToLookAt(camera.position, camera.direction, camera.up);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.NUM_2)){
            camera.position.set(46.05661f,3.8279848f,-66.23024f);
            camera.direction.set(0.102953315f,0.21813372f,0.9704725f);
            //camera.lookAt(0, 0, 0);
            camera.up.set(Vector3.Y); // Asegurar que el vector "up" de la cámara sea el eje Y
            camera.update();

            // Restablecer la matriz de transformación de la cámara
            camera.view.setToLookAt(camera.position, camera.direction, camera.up);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.NUM_3)){
            camera.position.set(54.125813f,5.176998f,-63.832558f);
            camera.direction.set(0.8894548f,-0.3648554f,0.27521577f);
            //camera.lookAt(0, 0, 0);
            camera.up.set(Vector3.Y); // Asegurar que el vector "up" de la cámara sea el eje Y
            camera.update();

            // Restablecer la matriz de transformación de la cámara
            camera.view.setToLookAt(camera.position, camera.direction, camera.up);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.NUM_4)){
            camera.position.set(55.900524f,4.9481387f,-60.274254f);
            camera.direction.set(0.8808824f,-0.35400352f,0.31420237f);
            //camera.lookAt(0, 0, 0);
            camera.up.set(Vector3.Y); // Asegurar que el vector "up" de la cámara sea el eje Y
            camera.update();

            // Restablecer la matriz de transformación de la cámara
            camera.view.setToLookAt(camera.position, camera.direction, camera.up);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.NUM_5)){
            camera.position.set(69.63072f,10.700968f,-37.33907f);
            camera.direction.set(-0.6464092f,-0.3375928f,-0.6842325f);
            //camera.lookAt(0, 0, 0);
            camera.up.set(Vector3.Y); // Asegurar que el vector "up" de la cámara sea el eje Y
            camera.update();

            // Restablecer la matriz de transformación de la cámara
            camera.view.setToLookAt(camera.position, camera.direction, camera.up);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.NUM_6)){
            camera.position.set(52.924725f,4.7397957f,-45.808308f);
            camera.direction.set(-0.9168267f,-0.14599568f,-0.37161097f);
            //camera.lookAt(0, 0, 0);
            camera.up.set(Vector3.Y); // Asegurar que el vector "up" de la cámara sea el eje Y
            camera.update();

            // Restablecer la matriz de transformación de la cámara
            camera.view.setToLookAt(camera.position, camera.direction, camera.up);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.NUM_7)){
            camera.position.set(49.44887f,4.8626556f,-51.611046f);
            camera.direction.set(0.9412464f,-0.33753186f,-0.010276056f);
            //camera.lookAt(0, 0, 0);
            camera.up.set(Vector3.Y); // Asegurar que el vector "up" de la cámara sea el eje Y
            camera.update();

            // Restablecer la matriz de transformación de la cámara
            camera.view.setToLookAt(camera.position, camera.direction, camera.up);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.NUM_8)){
            camera.position.set(49.23281f,4.5829105f,-54.456993f);
            camera.direction.set(0.93447727f,-0.2989261f,-0.19332094f);
            //camera.lookAt(0, 0, 0);
            camera.up.set(Vector3.Y); // Asegurar que el vector "up" de la cámara sea el eje Y
            camera.update();

            // Restablecer la matriz de transformación de la cámara
            camera.view.setToLookAt(camera.position, camera.direction, camera.up);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.NUM_9)){
            camera.position.set(33.590942f,5.209593f,-50.29063f);
            camera.direction.set(0.95255214f,-0.23728311f,-0.19057296f);
            //camera.lookAt(0, 0, 0);
            camera.up.set(Vector3.Y); // Asegurar que el vector "up" de la cámara sea el eje Y
            camera.update();

            // Restablecer la matriz de transformación de la cámara
            camera.view.setToLookAt(camera.position, camera.direction, camera.up);
        }
    }
    @Override
    public void dispose() {
        sceneManager.dispose();
        sceneAsset.dispose();
        environmentCubemap.dispose();
        diffuseCubemap.dispose();
        specularCubemap.dispose();
        brdfLUT.dispose();
        skybox.dispose();
    }
}