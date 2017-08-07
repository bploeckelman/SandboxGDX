package zendo.games.sandbox_gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import zendo.games.sandbox_gdx.screens.BaseScreen;
import zendo.games.sandbox_gdx.screens.TestScreen;
import zendo.games.sandbox_gdx.utils.Assets;

public class SandboxGdx extends ApplicationAdapter {

	public static SandboxGdx sandbox;

	private BaseScreen screen;

	@Override
	public void create () {
		Assets.load();
		float progress = 0f;
		do {
			progress = Assets.update();
		} while (progress != 1f);
		sandbox = this;

		setScreen(new TestScreen());
	}

	@Override
	public void render () {
		float dt = Math.min(Gdx.graphics.getDeltaTime(), 1f / 30f);
		Assets.tween.update(dt);
        screen.update(dt);
		screen.render(Assets.batch);
	}

	@Override
	public void dispose () {
		Assets.dispose();
	}

	public void setScreen(BaseScreen screen) {
		this.screen = screen;
	}

}
