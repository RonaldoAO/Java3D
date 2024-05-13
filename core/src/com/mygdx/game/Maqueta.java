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


public class Maqueta extends ApplicationAdapter
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
        sceneAsset = new GLTFLoader().load(Gdx.files.internal("models/scene.gltf"));
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
            camera.position.set(106.164246f,21.301752f,-60.25742f);
            camera.direction.set(-0.97139394f,-0.23743708f,0.004101047f);
            //camera.lookAt(0, 0, 0);
            camera.up.set(Vector3.Y); // Asegurar que el vector "up" de la cámara sea el eje Y
            camera.update();

            // Restablecer la matriz de transformación de la cámara
            camera.view.setToLookAt(camera.position, camera.direction, camera.up);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.NUM_2)){
            camera.position.set(75.98065f,11.08848f,-56.93991f);
            camera.direction.set(-0.9771805f,-0.20907982f,-0.037460044f);
            //camera.lookAt(0, 0, 0);
            camera.up.set(Vector3.Y); // Asegurar que el vector "up" de la cámara sea el eje Y
            camera.update();

            // Restablecer la matriz de transformación de la cámara
            camera.view.setToLookAt(camera.position, camera.direction, camera.up);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.NUM_3)){
            camera.position.set(83.36602f,18.595716f,-86.87038f);
            camera.direction.set(-0.814161f,-0.28235167f,0.5073639f);
            //camera.lookAt(0, 0, 0);
            camera.up.set(Vector3.Y); // Asegurar que el vector "up" de la cámara sea el eje Y
            camera.update();

            // Restablecer la matriz de transformación de la cámara
            camera.view.setToLookAt(camera.position, camera.direction, camera.up);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.NUM_4)){
            camera.position.set(27.786724f,12.770562f,-50.743965f);
            camera.direction.set(0.5970268f,-0.16902693f,-0.78420645f);
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