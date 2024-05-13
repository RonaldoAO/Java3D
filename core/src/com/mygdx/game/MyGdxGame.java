package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

public class MyGdxGame extends ApplicationAdapter {
	static class GameObject extends ModelInstance implements Disposable{
		public final btCollisionObject body;
		public boolean moving;
		public GameObject(Model model, String node, btCollisionShape shape) {
			super(model, node);
			this.body = new btCollisionObject();
			body.setCollisionShape(shape);
		}

		@Override
		public void dispose() {
			body.dispose();
		}

		static class Constructor implements  Disposable{

			public final Model model;
			public final String node;
			public final btCollisionShape shape;

			public Constructor (Model model, String node, btCollisionShape shape) {
				this.model = model;
				this.node = node;
				this.shape = shape;
			}

			public GameObject construct () {
				return new GameObject(model, node, shape);
			}

			@Override
			public void dispose () {
				shape.dispose();
			}
		}
	}
	PerspectiveCamera cam;
	CameraInputController camController;
	ModelBatch modelBatch;
	Array<ModelInstance> instances;
	Environment environment;

	Model model;
	ModelInstance ground;
	ModelInstance ball;

	boolean collsion;
	btCollisionShape groundShape;
	btCollisionShape ballShape;
	btCollisionObject groundObject;
	btCollisionObject ballObject;

	btCollisionConfiguration collisionConfig;
	btDispatcher dispatcher;
	
	@Override
	public void create () {
		modelBatch = new ModelBatch();
		Bullet.init();

		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

		cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(3f, 7f, 10f);
		cam.lookAt(0, 4f, 0 );
		cam.update();

		camController = new CameraInputController(cam);
		Gdx.input.setInputProcessor(camController);

		ModelBuilder mb = new ModelBuilder();
		mb.begin();
		mb.node().id = "ground";
		mb.part("box", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
				new Material(ColorAttribute.createDiffuse(Color.RED))).box(5f,1f, 5f);

		mb.node().id = "ball";
		mb.part("sphere", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
						new Material(ColorAttribute.createDiffuse(Color.GREEN)))
				.sphere(1f, 1f, 1f, 10, 10);
		model = mb.end();

		ground = new ModelInstance(model, "ground");
		ball = new ModelInstance(model, "ball");
		ball.transform.setToTranslation(0, 9f, 0);

		instances = new Array<ModelInstance>();

		ballShape = new btSphereShape(0.5f);
		groundShape = new btBoxShape(new Vector3(2.5f, 0.5f, 2.5f));

		groundObject = new btCollisionObject();
		groundObject.setCollisionShape(groundShape);
		groundObject.setWorldTransform(ground.transform);

		ballObject = new btCollisionObject();
		ballObject.setCollisionShape(ballShape);
		ballObject.setWorldTransform(ball.transform);

		collisionConfig = new btDefaultCollisionConfiguration();
		dispatcher = new btCollisionDispatcher(collisionConfig);

		instances.add(ground);
		instances.add(ball);
	}

	@Override
	public void render () {
		final float delta = Math.min(1f/30f, Gdx.graphics.getDeltaTime());
		if(!collsion){
			ball.transform.translate(0f, -delta, 0f);
			ballObject.setWorldTransform(ball.transform);
			collsion = checkCollision(ballObject, groundObject);
		}
		camController.update();

		Gdx.gl.glClearColor(0.3f, 0.3f, 0.3f, 1.f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		modelBatch.begin(cam);
		modelBatch.render(instances, environment);
		modelBatch.end();
	}

	private boolean checkCollision(btCollisionObject obj0, btCollisionObject obj1) {
		CollisionObjectWrapper co0 = new CollisionObjectWrapper(ballObject);
		CollisionObjectWrapper co1 = new CollisionObjectWrapper(groundObject);

		btCollisionAlgorithmConstructionInfo ci = new btCollisionAlgorithmConstructionInfo();
		ci.setDispatcher1(dispatcher);
		btCollisionAlgorithm algorithm = new btSphereBoxCollisionAlgorithm(null, ci, co0.wrapper, co1.wrapper, false);
		btDispatcherInfo info = new btDispatcherInfo();
		btManifoldResult result = new btManifoldResult(co0.wrapper, co1.wrapper);

		algorithm.processCollision(co0.wrapper, co1.wrapper, info, result);

		boolean r = result.getPersistentManifold().getNumContacts() > 0;

		result.dispose();
		info.dispose();
		algorithm.dispose();
		ci.dispose();
		co1.dispose();
		co0.dispose();

		return r;

	}

	@Override
	public void dispose () {
		groundObject.dispose();
		groundShape.dispose();

		ballObject.dispose();
		ballShape.dispose();

		dispatcher.dispose();
		collisionConfig.dispose();

		modelBatch.dispose();
		model.dispose();
	}
}
