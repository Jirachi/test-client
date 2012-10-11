package edu.test.client;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import android.os.Bundle;
import android.util.Log;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.WorldManifold;
import com.badlogic.gdx.utils.Array;

public class GameClientActivity  extends AndroidApplication {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = true;
        config.useCompass = false;
        config.useWakelock = true;
        config.useGL20 = true;
        initialize(new GameClient(), config);
    }


	public class GameClient implements ApplicationListener, InputProcessor {
		final static float MAX_VELOCITY = 7f;		
		boolean jump = false;	
		World world;
		Body player;
		Body coop;
		Fixture playerPhysicsFixture;
		Fixture playerSensorFixture;
		Fixture coopPhysicsFixture;
		Fixture coopSensorFixture;
		OrthographicCamera cam;
		Box2DDebugRenderer renderer;
		Array<MovingPlatform> platforms = new Array<MovingPlatform>();
		MovingPlatform groundedPlatform = null;
		float stillTime = 0;
		long lastGroundTime = 0;
		long coopLastGroundTime = 0;
		SpriteBatch batch;
		private Texture pechouchouxTex;           // #1
		private Texture longueloisTex;           // #1
		BitmapFont font;
		boolean pressedLeft = false;
		boolean pressedRight = false;
		boolean coopLeft = false;
		boolean coopRight = false;
		
		long lastTapTime = 0;
		Socket mSocket = null;
		BufferedReader mNetReader = null;
		BufferedOutputStream mNetWriter = null;
		
		long lastPosSync = 0;
		
		@Override
		public void create() {
			world = new World(new Vector2(0, -20), true);		
			renderer = new Box2DDebugRenderer();
			cam = new OrthographicCamera(28, 20);
			cam.update();
			createWorld();
			Gdx.input.setInputProcessor(this);
			batch = new SpriteBatch();
			font = new BitmapFont();

	        InputStream input   = null;
	        try
	        {
	            int _port   = 8080;
	            mSocket = new Socket("192.168.0.10", _port);
	            
	            // Open stream
	            input = mSocket.getInputStream();
	            
	            
	            // Show the server response
	            mNetReader = new BufferedReader(new InputStreamReader(input));
	            mNetWriter = new BufferedOutputStream(mSocket.getOutputStream());
	        }
	        catch (UnknownHostException e)
	        {
	            e.printStackTrace();
	        }
	        catch (IOException e)
	        {
	            e.printStackTrace();
	        }
	        
		}
	
		private void createWorld() {
			float y1 = 1; //(float)Math.random() * 0.1f + 1;
			float y2 = y1;
			for(int i = 0; i < 50; i++) {
				Body ground = createEdge(BodyType.StaticBody, -50 + i * 2, y1, -50 + i * 2 + 2, y2, 0);			
				y1 = y2;
				y2 = 1; //(float)Math.random() + 1;
			}			
	 
			Body box = createBox(BodyType.StaticBody, 1, 1, 0);
			box.setTransform(30, 3, 0);
			box = createBox(BodyType.StaticBody, 1.2f, 1.2f, 0);
			box.setTransform(5, 2.4f, 0);
			player = createPlayer();
			player.setTransform(10.0f, 4.0f, 0);
			player.setFixedRotation(true);						
	 
			for(int i = 0; i < 20; i++) {
				box = createBox(BodyType.DynamicBody, (float)Math.random(), (float)Math.random(), 3);
				box.setTransform((float)Math.random() * 10f - (float)Math.random() * 10f, (float)Math.random() * 10 + 6, (float)(Math.random() * 2 * Math.PI));
			}
	 
			for(int i = 0; i < 20; i++) {
				Body circle = createCircle(BodyType.DynamicBody, (float)Math.random() * 0.5f, 3);
				circle.setTransform((float)Math.random() * 10f - (float)Math.random() * 10f, (float)Math.random() * 10 + 6, (float)(Math.random() * 2 * Math.PI));
			}
	 
			//platforms.add(new MovingPlatform(-2, 3, 2, 0.5f, 2, 0, 4));
			//platforms.add(new MovingPlatform(17, 3, 5, 0.5f, 0, 2, 5));		
//			platforms.add(new MovingPlatform(-7, 5, 2, 0.5f, -2, 2, 8));		
//			platforms.add(new MovingPlatform(40, 3, 20, 0.5f, 0, 2, 5));
		}
	 
		private Body createBox(BodyType type, float width, float height, float density) {
			BodyDef def = new BodyDef();
			def.type = type;
			Body box = world.createBody(def);
	 
			PolygonShape poly = new PolygonShape();
			poly.setAsBox(width, height);
			box.createFixture(poly, density);
			poly.dispose();
	 
			return box;
		}	
	 
		private Body createEdge(BodyType type, float x1, float y1, float x2, float y2, float density) {
			BodyDef def = new BodyDef();
			def.type = type;
			Body box = world.createBody(def);
	 
			EdgeShape poly = new EdgeShape();
			poly.set(new Vector2(0, 0), new Vector2(x2 - x1, y2 - y1));
			box.createFixture(poly, density);
			box.setTransform(x1, y1, 0);
			poly.dispose();
	 
			return box;
		}
	 
		private Body createCircle(BodyType type, float radius, float density) {
			BodyDef def = new BodyDef();
			def.type = type;
			Body box = world.createBody(def);
	 
			CircleShape poly = new CircleShape();
			poly.setRadius(radius);
			box.createFixture(poly, density);
			poly.dispose();
	 
			return box;
		}	
	 
		private Body createPlayer() {
			BodyDef def = new BodyDef();
			def.type = BodyType.DynamicBody;
			Body box = world.createBody(def);
	 
			PolygonShape poly = new PolygonShape();		
			poly.setAsBox(0.45f, 1.4f);
			playerPhysicsFixture = box.createFixture(poly, 1);
			poly.dispose();			
	 
			CircleShape circle = new CircleShape();		
			circle.setRadius(0.45f);
			circle.setPosition(new Vector2(0, -1.4f));
			playerSensorFixture = box.createFixture(circle, 0);		
			circle.dispose();		
	 
			box.setBullet(true);
			
			pechouchouxTex = new Texture(Gdx.files.internal("gfx/pechouchoux.png"));
	 
			return box;
		}
		
		private Body createCoop() {
			BodyDef def = new BodyDef();
			def.type = BodyType.DynamicBody;
			Body box = world.createBody(def);
	 
			PolygonShape poly = new PolygonShape();		
			poly.setAsBox(0.45f, 1.4f);
			coopPhysicsFixture = box.createFixture(poly, 1);
			poly.dispose();			
	 
			CircleShape circle = new CircleShape();		
			circle.setRadius(0.45f);
			circle.setPosition(new Vector2(0, -1.4f));
			coopSensorFixture = box.createFixture(circle, 0);		
			circle.dispose();		
	 
			box.setBullet(true);
			
			longueloisTex = new Texture(Gdx.files.internal("gfx/longuelois.png"));
	 
			return box;
		}
	 
		
		@Override
		public void resize(int width, int height) {
			// TODO Auto-generated method stub
		}
	
		@Override
		public void render() {
			Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
			cam.position.set(player.getPosition().x, player.getPosition().y, 0);
			cam.update();
			//cam.apply(Gdx.gl10);
			renderer.render(world, cam.combined);
			
			// network operations
			try {
				if (mNetReader.ready()) {
					String response = mNetReader.readLine().trim();
					Log.e("network", "Server message: " + response);
					
					if (response.equals("CONNECT")) {
						coop = createCoop();
						coop.setTransform(20.0f, 4.0f, 0);
						coop.setFixedRotation(true);		
					}
					else if (response.equals("ML")) {
						coopLeft = true;
						coopRight = false;
					}
					else if (response.equals("MR")) {
						coopRight = true;
						coopLeft = false;
					}
					else if (response.equals("STAHP")) // ;_;
					{
						coopLeft = false;
						coopRight = false;
					}
					else if (response.startsWith("SYNC;")) {
						String[] values = response.split(";");
						float posX = Float.parseFloat(values[1]);
    					float posY = Float.parseFloat(values[2]);
    					
    					coop.setTransform(posX,posY,0);
					}
				}
			}
			catch (IOException e) {
				Log.e("network", e.toString());
			}
            
	 
			// ==================================
			// PLAYER
			// ==================================
			Vector2 vel = player.getLinearVelocity();
			Vector2 pos = player.getPosition();		
			boolean grounded = isPlayerGrounded(Gdx.graphics.getDeltaTime());
			if(grounded) {
				lastGroundTime = System.nanoTime();
			} else {
				if(System.nanoTime() - lastGroundTime < 100000000) {
					grounded = true;
				}
			}
	 
			// cap max velocity on x		
			if(Math.abs(vel.x) > MAX_VELOCITY) {			
				vel.x = Math.signum(vel.x) * MAX_VELOCITY;
				player.setLinearVelocity(vel.x, vel.y);
			}
	 
			// calculate stilltime & damp
			if(!pressedLeft && !pressedRight) {			
				stillTime += Gdx.graphics.getDeltaTime();
				player.setLinearVelocity(vel.x * 0.9f, vel.y);
			}
			else { 
				stillTime = 0;
			}			
	 
			// disable friction while jumping
			if(!grounded) {			
				playerPhysicsFixture.setFriction(0f);
				playerSensorFixture.setFriction(0f);			
			} else {
				if(!pressedLeft && !pressedRight && stillTime > 0.2) {
					playerPhysicsFixture.setFriction(100f);
					playerSensorFixture.setFriction(100f);
				}
				else {
					playerPhysicsFixture.setFriction(0.2f);
					playerSensorFixture.setFriction(0.2f);
				}
	 
				if(groundedPlatform != null && groundedPlatform.dist == 0) {
					player.applyLinearImpulse(0, -24, pos.x, pos.y);				
				}
			}		
	 
			// apply left impulse, but only if max velocity is not reached yet
			if(pressedLeft && vel.x > -MAX_VELOCITY) {
				player.applyLinearImpulse(-2f, 0, pos.x, pos.y);
			} 
	 
			// apply right impulse, but only if max velocity is not reached yet
			if(pressedRight && vel.x < MAX_VELOCITY) {
				player.applyLinearImpulse(2f, 0, pos.x, pos.y);
			}

			// jump, but only when grounded
			if(jump) {			
				jump = false;
				if(grounded) {
					player.setLinearVelocity(vel.x, 0);			
					System.out.println("jump before: " + player.getLinearVelocity());
					player.setTransform(pos.x, pos.y + 0.01f, 0);
					player.applyLinearImpulse(0, 30, pos.x, pos.y);			
					System.out.println("jump, " + player.getLinearVelocity());				
				}
			}					
			
			if (System.currentTimeMillis() - lastPosSync > 500) {
				// sync pos
				try {
					mNetWriter.write(new String("SYNC;" + pos.x + ";" + pos.y + "\n").getBytes());
					mNetWriter.flush();
				} catch (IOException e) { e.printStackTrace(); }
				
				lastPosSync = System.currentTimeMillis();
			}
			

			// ==================================
			// COOP PLAYER
			// ==================================
			if (coop != null) {
				vel = coop.getLinearVelocity();
				pos = coop.getPosition();		
				grounded = isCoopGrounded(Gdx.graphics.getDeltaTime());
				if(grounded) {
					coopLastGroundTime = System.nanoTime();
				} else {
					if(System.nanoTime() - lastGroundTime < 100000000) {
						grounded = true;
					}
				}
		 
				// cap max velocity on x		
				if(Math.abs(vel.x) > MAX_VELOCITY) {			
					vel.x = Math.signum(vel.x) * MAX_VELOCITY;
					coop.setLinearVelocity(vel.x, vel.y);
				}
		 
				// calculate stilltime & damp
				if(!coopLeft && !coopRight) {			
					stillTime += Gdx.graphics.getDeltaTime();
					coop.setLinearVelocity(vel.x * 0.9f, vel.y);
				}
				else { 
					stillTime = 0;
				}			
		 
				// disable friction while jumping
				if(!grounded) {			
					coopPhysicsFixture.setFriction(0f);
					coopSensorFixture.setFriction(0f);			
				} else {
					if(!coopLeft && !coopRight && stillTime > 0.2) {
						coopPhysicsFixture.setFriction(100f);
						coopSensorFixture.setFriction(100f);
					}
					else {
						coopPhysicsFixture.setFriction(0.2f);
						coopSensorFixture.setFriction(0.2f);
					}
		 
					if(groundedPlatform != null && groundedPlatform.dist == 0) {
						coop.applyLinearImpulse(0, -24, pos.x, pos.y);				
					}
				}		
		 
				// apply left impulse, but only if max velocity is not reached yet
				if(coopLeft && vel.x > -MAX_VELOCITY) {
					coop.applyLinearImpulse(-2f, 0, pos.x, pos.y);
				}
		 
				// apply right impulse, but only if max velocity is not reached yet
				if(coopRight && vel.x < MAX_VELOCITY) {
					coop.applyLinearImpulse(2f, 0, pos.x, pos.y);
				}
			}

			// ======================================================
	 
			vel = player.getLinearVelocity();
			pos = player.getPosition();		
	 
			// update platforms
			for(int i = 0; i < platforms.size; i++) {
				MovingPlatform platform = platforms.get(i);
				platform.update(Math.max(1/30.0f, Gdx.graphics.getDeltaTime()));
			}
			
			float accelX = Gdx.input.getAccelerometerX();
		    float accelY = Gdx.input.getAccelerometerY();
		    //world.setGravity(new Vector2(accelY * 0.5f, -accelX * 2.0f));
	 
			// le step...			
			world.step(Gdx.graphics.getDeltaTime(), 4, 4);
			player.setAwake(true);
			
			
			cam.project(point.set(pos.x, pos.y, 0));
			batch.begin();
			font.drawMultiLine(batch, "friction: " + playerPhysicsFixture.getFriction() + "\ngrounded: " + grounded, point.x+20, point.y);
			batch.draw(pechouchouxTex, Gdx.graphics.getWidth()/2-55, Gdx.graphics.getHeight()/2-65, 110, 130);
			
			if (longueloisTex != null) {
				cam.project(point.set(coop.getPosition().x, coop.getPosition().y, 0));
				batch.draw(longueloisTex, point.x-55, point.y-65, 110, 130);
			}
			
			batch.end();
		}
		
		private boolean isPlayerGrounded(float deltaTime) {				
			groundedPlatform = null;
			List<Contact> contactList = world.getContactList();
			for(int i = 0; i < contactList.size(); i++) {
				Contact contact = contactList.get(i);
				if(contact.isTouching() && (contact.getFixtureA() == playerSensorFixture ||
				   contact.getFixtureB() == playerSensorFixture)) {				
	 
					Vector2 pos = player.getPosition();
					WorldManifold manifold = contact.getWorldManifold();
					boolean below = true;
					for(int j = 0; j < manifold.getNumberOfContactPoints(); j++) {
						below &= (manifold.getPoints()[j].y < pos.y - 1.5f);
					}
	 
					if(below) {
						if(contact.getFixtureA().getUserData() != null && contact.getFixtureA().getUserData().equals("p")) {
							groundedPlatform = (MovingPlatform)contact.getFixtureA().getBody().getUserData();							
						}
	 
						if(contact.getFixtureB().getUserData() != null && contact.getFixtureB().getUserData().equals("p")) {
							groundedPlatform = (MovingPlatform)contact.getFixtureB().getBody().getUserData();
						}											
						return true;			
					}
	 
					return false;
				}
			}
			return false;
		}
		
		private boolean isCoopGrounded(float deltaTime) {				
			groundedPlatform = null;
			List<Contact> contactList = world.getContactList();
			for(int i = 0; i < contactList.size(); i++) {
				Contact contact = contactList.get(i);
				if(contact.isTouching() && (contact.getFixtureA() == coopSensorFixture ||
				   contact.getFixtureB() == coopSensorFixture)) {				
	 
					Vector2 pos = coop.getPosition();
					WorldManifold manifold = contact.getWorldManifold();
					boolean below = true;
					for(int j = 0; j < manifold.getNumberOfContactPoints(); j++) {
						below &= (manifold.getPoints()[j].y < pos.y - 1.5f);
					}
	 
					if(below) {
						if(contact.getFixtureA().getUserData() != null && contact.getFixtureA().getUserData().equals("p")) {
							groundedPlatform = (MovingPlatform)contact.getFixtureA().getBody().getUserData();							
						}
	 
						if(contact.getFixtureB().getUserData() != null && contact.getFixtureB().getUserData().equals("p")) {
							groundedPlatform = (MovingPlatform)contact.getFixtureB().getBody().getUserData();
						}											
						return true;			
					}
	 
					return false;
				}
			}
			return false;
		}
	
		@Override
		public void pause() {
			// TODO Auto-generated method stub
		}
	
		@Override
		public void resume() {
			// TODO Auto-generated method stub
		}
	
		@Override
		public void dispose() {
			// TODO Auto-generated method stub
		}
		
		 
		Vector2 last = null;
		Vector3 point = new Vector3();
		
		@Override
		public boolean touchDown(int x, int y, int pointerId, int button) {
			/*cam.unproject(point.set(x, y, 0));
	 
			if(button == Input.Buttons.LEFT) {
				if(last == null) {
					last = new Vector2(point.x, point.y);
				} else {
					createEdge(BodyType.StaticBody, last.x, last.y, point.x, point.y, 0);
					last.set(point.x, point.y);
				}
			} else {
				last = null;
			}*/
			
			//cam.unproject(point.set(x,y,0));
			
			if (x <= Gdx.graphics.getWidth()/2) {
				pressedLeft = true;
				pressedRight = false;
				try {
					mNetWriter.write(new String("ML\n").getBytes());
					mNetWriter.flush();
				} catch (IOException e) { e.printStackTrace(); }
			} else {
				pressedLeft = false;
				pressedRight = true;
				try {
					mNetWriter.write(new String("MR\n").getBytes());
					mNetWriter.flush();
				} catch (IOException e) { e.printStackTrace(); }
			}
		
			if (System.currentTimeMillis() - lastTapTime <= 100) {
				jump = true;	
			}
			
			Log.i("test", "Delta time: " + (System.currentTimeMillis() - lastTapTime) + "ms");
			
			lastTapTime = System.currentTimeMillis();
		
	 
			return false;
		}
		
		class MovingPlatform {
			Body platform;		
			Vector2 pos = new Vector2();
			Vector2 dir = new Vector2();
			float dist = 0;
			float maxDist = 0;		
	 
			public MovingPlatform(float x, float y, float width, float height, float dx, float dy, float maxDist) {
				platform = createBox(BodyType.KinematicBody, width, height, 1);			
				pos.x = x;
				pos.y = y;
				dir.x = dx;
				dir.y = dy;
				this.maxDist = maxDist;
				platform.setTransform(pos, 0);
				platform.getFixtureList().get(0).setUserData("p");
				platform.setUserData(this);
			}
	 
			public void update(float deltaTime) {
				dist += dir.len() * deltaTime;
				if(dist > maxDist) {
					dir.mul(-1);
					dist = 0;
				}
	 
				platform.setLinearVelocity(dir);			
			}
		}

		@Override
		public boolean keyDown(int arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean keyTyped(char arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean keyUp(int arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean scrolled(int arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean touchDragged(int arg0, int arg1, int arg2) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean touchMoved(int arg0, int arg1) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean touchUp(int arg0, int arg1, int arg2, int arg3) {
			pressedLeft = false;
			pressedRight = false;
			lastTapTime = System.currentTimeMillis();
			
			try {
				mNetWriter.write(new String("STAHP\n").getBytes()); // ;_;
				mNetWriter.flush();
			} catch (IOException e) { e.printStackTrace(); }
			
			return false;
		}
	}
}