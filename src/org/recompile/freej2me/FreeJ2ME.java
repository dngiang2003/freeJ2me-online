/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package org.recompile.freej2me;

/*
	FreeJ2ME - AWT
*/

import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;
import org.recompile.mobile.PlatformGraphics;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;

import javax.imageio.ImageIO;

public class FreeJ2ME
{

	public static FreeJ2ME app;
	protected Frame main;
	private int lcdWidth;
	private int lcdHeight;
	private int scaleFactor = 1;

	private static final String extInputFilePath = "FreeJ2MEExternalKeyEvents.txt";
	private static final HashMap<String, Integer> extEventsMap = new HashMap<String, Integer>();
	private static BufferedReader extEventReader;

	// Add all expected key inputs
	static
	{
		extEventsMap.put("k0", 0);
		extEventsMap.put("k1", 0);
		extEventsMap.put("k2", 0);
		extEventsMap.put("k3", 0);
		extEventsMap.put("k4", 0);
		extEventsMap.put("k5", 0);
		extEventsMap.put("k6", 0);
		extEventsMap.put("k7", 0);
		extEventsMap.put("k8", 0);
		extEventsMap.put("k9", 0);
		extEventsMap.put("ka", 0);
		extEventsMap.put("kb", 0);
		extEventsMap.put("ku", 0);
		extEventsMap.put("kd", 0);
		extEventsMap.put("kl", 0);
		extEventsMap.put("kr", 0);
		extEventsMap.put("kc", 0);
		extEventsMap.put("ls", 0);
		extEventsMap.put("rs", 0);
		extEventsMap.put("cl", 0);
		extEventsMap.put("ff", 0);
		extEventsMap.put("ro", 0);
		extEventsMap.put("pa", 0);
	}

	public static final Color freeJ2MEBGColor = new Color(0,0,64, 255);
	public static final Color freeJ2MEDragColor = new Color(55, 55, 125, 255);

	public static boolean isFullscreen = false;

	private LCD lcd;

	private int xborder;
	private int yborder;

	// AWT GUI
	private AWTGUI awtGUI;

	// Tracks pressed state of forwarded text keys (ASCII 32..126 + backspace)
	private final boolean[] textKeysPressed = new boolean[128];

	public static void main(String args[])
	{
		Mobile.clearOldLog();
		FreeJ2ME.app = new FreeJ2ME(args);

		// After FreeJ2ME is properly opened, start the external input thread
		try { checkExtInputFile(); }
		catch(IOException e) { Mobile.log(Mobile.LOG_ERROR, FreeJ2ME.class.getPackage().getName() + "." + FreeJ2ME.class.getSimpleName() + ": " + "Couldn't setup external input reader..."); }
	}

	private static void checkExtInputFile() throws IOException
	{
		// Begin checking if this is the web frontend, which always has the file present at boot
		File extFile = new File("/str/"+extInputFilePath);

		// If File doesn't exist on that dir, we're running standalone. (TODO: Using a pipe for this would be better on standalone)
		if(!extFile.exists())
		{
			return;
			//extFile = new File("freej2me_system/"+extInputFilePath);
			//extFile.createNewFile();
		}

		final String filePath = extFile.getPath();

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				while (true)
				{
					readFile(filePath);
					try { Thread.sleep(4); } // External inputs poll at a 250fps rate, more than fast enough for just about everything
					catch (InterruptedException e) { }
				}
			}
		}, "ExternalInputs-Thread").start();
    }

    private static void readFile(String filePath)
	{
        try
		{
            String line;
			extEventReader = new BufferedReader(new FileReader(filePath));
            while ((line = extEventReader.readLine()) != null)
			{
                String[] parts = line.split(":");
                if (parts.length == 2)
				{
                    String key = parts[0].trim();
					int value = Integer.parseInt(parts[1].trim());
                    if(value != extEventsMap.get(key))
					{
						extEventsMap.replace(key, value);
						processExternalKey(key, value);
					}
                }
            }
			extEventReader.close();
        } catch (IOException e) { e.printStackTrace(); }
    }

	private static void processExternalKey(String strkey, int value)
	{
		int key = 0; // k0
		if(strkey.equals("k1"))      { key = 1; }
		else if(strkey.equals("k2")) { key = 2; }
		else if(strkey.equals("k3")) { key = 3; }
		else if(strkey.equals("k4")) { key = 4; }
		else if(strkey.equals("k5")) { key = 5; }
		else if(strkey.equals("k6")) { key = 6; }
		else if(strkey.equals("k7")) { key = 7; }
		else if(strkey.equals("k8")) { key = 8; }
		else if(strkey.equals("k9")) { key = 9; }
		else if(strkey.equals("k*")) { key = 10; }
		else if(strkey.equals("k#")) { key = 11; }
		else if(strkey.equals("ku")) { key = 12; }
		else if(strkey.equals("kd")) { key = 13; }
		else if(strkey.equals("kl")) { key = 14; }
		else if(strkey.equals("kr")) { key = 15; }
		else if(strkey.equals("kc")) { key = 16; }
		else if(strkey.equals("ls")) { key = 17; }
		else if(strkey.equals("rs")) { key = 18; }
		else if(strkey.equals("cl")) { key = 19; }
		else if(strkey.equals("ff")) { key = 20; }
		else if(strkey.equals("ro")) { key = 21; }
		else if(strkey.equals("pa")) { key = 22; }

		switch(key)
			{
				case 0:  // 0 - k0
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD0, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD0, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 1: // 1 - k1
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD1, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD1, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 2: // 2 (8 in keyboard numpad) - k2
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD8, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD8, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 3:
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD3, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD3, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 4:
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD4, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD4, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 5:
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD5, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD5, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 6:
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD6, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD6, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 7:
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD7, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD7, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 8:
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD2, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD2, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 9: // k9
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD9, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_NUMPAD9, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 10: // k*
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_E, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_E, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 11: // k#
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_R, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_R, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 12: // Up - ku
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 13: // Down - kd
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 14: // Left - kl
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 15: // Right - kr
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 16: // Fire - kc
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ENTER, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 17: // leftSoft - ls
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_Q, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_Q, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 18: // rightSoft - rs
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_W, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_W, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 19: // CLR - cl
					if(value == 1) { app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_A, KeyEvent.CHAR_UNDEFINED), true); }
					else { app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_A, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 20: // Fast-Forward - ff
					if(value == 1 && !Mobile.isFastForwarding) { Mobile.isFastForwarding = true; app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_SPACE, KeyEvent.CHAR_UNDEFINED), true); }
					else if(value == 0 && Mobile.isFastForwarding) { Mobile.isFastForwarding = false; app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_SPACE, KeyEvent.CHAR_UNDEFINED)); }
					break;
				case 21: // Rotation - ro
					if(value == 1)
					{
						int rotation = Mobile.rotateDisplay + 90;
						if(rotation == 360) { rotation = 0; }
						Mobile.config.settings.put("rotate",  "" + rotation);
						app.settingsChanged();
					}
					break;
				case 22: // Pause - pa
					if(value == 1 && !Mobile.isPaused) { Mobile.isPaused = true; app.pressKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_X, KeyEvent.CHAR_UNDEFINED), true); }
					else if(value == 0 && Mobile.isPaused) { Mobile.isPaused = false; app.releaseKey(new KeyEvent(app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_X, KeyEvent.CHAR_UNDEFINED)); }
					break;
			}
	}

	public static void closeApp()
	{
		try
		{
            String java = System.getProperty("java.home") + "/bin/java";
            String classPath = System.getProperty("java.class.path");

            String[] commands = new String[] { java, "-Dfile.encoding="+Mobile.textEncoding, "-cp", classPath, FreeJ2ME.class.getName() };

            // Start a new instance
            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            processBuilder.start();

            // Exit the current instance
            System.exit(0);
        }
		catch (IOException e) { e.printStackTrace(); }
	}

	public FreeJ2ME(String args[])
	{
		// Setup Device //
		boolean fullscreenAtStartup = false;
		if(args.length>=1)
		{
			try { MobilePlatform.fileName = getFormattedLocation(URLDecoder.decode(args[0], Mobile.textEncoding)); }
			catch(Exception e) { }
		}

		if(args.length>=2)
		{
			fullscreenAtStartup = (Integer.parseInt(args[1]) == 1);
		}

		if(args.length>=4)
		{
			Mobile.lcdWidth = Integer.parseInt(args[2]);
			Mobile.lcdHeight = Integer.parseInt(args[3]);
		}
		if(args.length>=5)
		{
			scaleFactor = Integer.parseInt(args[4]);
		}

		lcdWidth = Mobile.lcdWidth;
		lcdHeight = Mobile.lcdHeight;

		Mobile.setPlatform(new MobilePlatform(lcdWidth, lcdHeight), new Runnable() { public void run() { settingsChanged(); } });

		lcd = new LCD();
		lcd.setFocusable(true);

		awtGUI = new AWTGUI(Mobile.config);

		constructFreeJ2MEGUI();

		// Only now we can load the jar passed as argument
		if(MobilePlatform.fileName != null) { awtGUI.loadJarFile(MobilePlatform.fileName); }

		/* Inputs should only be registered if a jar has been loaded, otherwise AWT will throw NullPointerException */
		lcd.addKeyListener(new KeyListener()
		{
			public void keyPressed(KeyEvent e) { pressKey(e, false); }

			public void keyReleased(KeyEvent e) { releaseKey(e); }

			public void keyTyped(KeyEvent e) { }

		});

		lcd.addMouseListener(new MouseListener()
		{

			public void mousePressed(MouseEvent e)
			{
				if(awtGUI.hasLoadedFile())
				{
					int x = (int)((e.getX()-lcd.cx) * lcd.scalex);
					int y = (int)((e.getY()-lcd.cy) * lcd.scaley);

					// Adjust the pointer coords if the screen is rotated, same for mouseReleased
					if(Mobile.rotateDisplay == 90)
					{
						x = (int)((e.getY() - lcd.cy) * lcd.scalex);
						y = (int)((lcd.cw - (e.getX() - lcd.cx)) * lcd.scaley);
					}
					if(Mobile.rotateDisplay == 180)
					{
						x = (int)((lcd.cw - (e.getX() - lcd.cx)) * lcd.scalex);
						y = (int)((lcd.ch - (e.getY() - lcd.cy)) * lcd.scaley);
					}
					if(Mobile.rotateDisplay == 270)
					{
						x = (int)((lcd.ch - (e.getY() - lcd.cy)) * lcd.scaley);
						y = (int)((e.getX() - lcd.cx) * lcd.scalex);
					}

					MobilePlatform.pointerPressed(x, y);
				}
			}

			public void mouseReleased(MouseEvent e)
			{
				if(awtGUI.hasLoadedFile())
				{
					int x = (int)((e.getX()-lcd.cx) * lcd.scalex);
					int y = (int)((e.getY()-lcd.cy) * lcd.scaley);

					if(Mobile.rotateDisplay == 90)
					{
						x = (int)((e.getY() - lcd.cy) * lcd.scalex);
						y = (int)((lcd.cw - (e.getX() - lcd.cx)) * lcd.scaley);
					}
					if(Mobile.rotateDisplay == 180)
					{
						x = (int)((lcd.cw - (e.getX() - lcd.cx)) * lcd.scalex);
						y = (int)((lcd.ch - (e.getY() - lcd.cy)) * lcd.scaley);
					}
					if(Mobile.rotateDisplay == 270)
					{
						x = (int)((lcd.ch - (e.getY() - lcd.cy)) * lcd.scaley);
						y = (int)((e.getX() - lcd.cx) * lcd.scalex);
					}

					MobilePlatform.pointerReleased(x, y);
				}
			}

			public void mouseExited(MouseEvent e) { }
			public void mouseEntered(MouseEvent e) { }
			public void mouseClicked(MouseEvent e) { }

		});

		lcd.addMouseMotionListener(new MouseMotionAdapter()
		{
			public void mouseDragged(MouseEvent e)
			{
				if(awtGUI.hasLoadedFile())
				{
					int x = (int)((e.getX()-lcd.cx) * lcd.scalex);
					int y = (int)((e.getY()-lcd.cy) * lcd.scaley);

					if(Mobile.rotateDisplay == 90)
					{
						x = (int)((e.getY() - lcd.cy) * lcd.scalex);
						y = (int)((lcd.cw - (e.getX() - lcd.cx)) * lcd.scaley);
					}
					if(Mobile.rotateDisplay == 180)
					{
						x = (int)((lcd.cw - (e.getX() - lcd.cx)) * lcd.scalex);
						y = (int)((lcd.ch - (e.getY() - lcd.cy)) * lcd.scaley);
					}
					if(Mobile.rotateDisplay == 270)
					{
						x = (int)((lcd.ch - (e.getY() - lcd.cy)) * lcd.scaley);
						y = (int)((e.getX() - lcd.cx) * lcd.scalex);
					}

					MobilePlatform.pointerDragged(x, y);
				}
			}
		});

		displayGUI();

		// Set painter right before the jar is loaded
		Mobile.getPlatform().setPainter(new Runnable()
		{
			public void run()
			{
				/* Set menuBar option states based on loaded config */
				if(awtGUI.hasJustLoaded()) { awtGUI.updateOptions(); }

				/* Whenever AWT GUI notifies that its menu options were changed, update settings */
				if(awtGUI.hasChanged()) { settingsChanged(); awtGUI.clearChanged(); }

				lcd.repaint();
			}
		});

		if(args.length<1)
		{
			while(!awtGUI.hasLoadedFile())
			{
				try{ Thread.sleep(1000); }
				catch (InterruptedException e) { }
			}
		}
		if(Mobile.getPlatform().load(awtGUI.getJarPath()))
		{
			/* Allows FreeJ2ME to set the width and height passed as cmd arguments. */
			if(args.length>=4)
			{
				lcdWidth = Integer.parseInt(args[1]);
				lcdHeight = Integer.parseInt(args[2]);
				Mobile.config.settings.put("width",  ""+lcdWidth);
				Mobile.config.settings.put("height", ""+lcdHeight);
			}

			if(args.length>=6)
			{
				if(Integer.parseInt(args[5]) == 0)  { Mobile.config.settings.put("phone",  "Standard"); }
				if(Integer.parseInt(args[5]) == 1)  { Mobile.config.settings.put("phone",  "LG"); }
				if(Integer.parseInt(args[5]) == 2)  { Mobile.config.settings.put("phone",  "Motorola"); }
				if(Integer.parseInt(args[5]) == 3)  { Mobile.config.settings.put("phone",  "MotoTriplets"); }
				if(Integer.parseInt(args[5]) == 4)  { Mobile.config.settings.put("phone",  "MotoV8"); }
				if(Integer.parseInt(args[5]) == 5)  { Mobile.config.settings.put("phone",  "MotoA1000"); }
				if(Integer.parseInt(args[5]) == 6)  { Mobile.config.settings.put("phone",  "NokiaKeyboard"); }
				if(Integer.parseInt(args[5]) == 7)  { Mobile.config.settings.put("phone",  "Sagem"); }
				if(Integer.parseInt(args[5]) == 8)  { Mobile.config.settings.put("phone",  "Siemens"); }
				if(Integer.parseInt(args[5]) == 9)  { Mobile.config.settings.put("phone",  "SKT"); }
				if(Integer.parseInt(args[5]) == 10) { Mobile.config.settings.put("phone",  "KDDI"); }
			}

			if(args.length>=7)
			{
				Mobile.config.settings.put("fps", ""+Integer.parseInt(args[6])+"");
			}

			if(args.length>=8)
			{
				Mobile.config.settings.put("dojaversion", ""+Integer.parseInt(args[7])+"");
			}

			settingsChanged();

			Mobile.getPlatform().runJar();
		}
		else
		{
			Mobile.log(Mobile.LOG_ERROR, FreeJ2ME.class.getPackage().getName() + "." + FreeJ2ME.class.getSimpleName() + ": " + "Couldn't load jar...");
		}

		// Go fullscreen as soon as the jar is loaded from the commandline path above
		if(fullscreenAtStartup) { toggleFullscreen(); }
	}

	protected void pressKey(KeyEvent e, boolean ignoreModifiers)
	{
		if(awtGUI.hasLoadedFile())
		{
			int keycode = e.getKeyCode();
			int mobikey = getMobileKey(keycode);

			switch(keycode) // Handle emulator control keys
			{
				case KeyEvent.VK_EQUALS:
				case KeyEvent.VK_ADD:
					if(!isFullscreen)
					{
						scaleFactor++;
						main.setSize(lcdWidth * scaleFactor + xborder, lcdHeight * scaleFactor + yborder);
					}
				break;
				case KeyEvent.VK_MINUS:
				case KeyEvent.VK_SUBTRACT:
					if(scaleFactor > 1 && !isFullscreen)
					{
						scaleFactor--;
						main.setSize(lcdWidth * scaleFactor + xborder, lcdHeight * scaleFactor + yborder);
					}
				break;
				case KeyEvent.VK_F:
					if(e.isAltDown() && e.isControlDown())
					{
						toggleFullscreen();
					}
				break;
				case KeyEvent.VK_R: // Toggle rotation
					if(e.isAltDown() && e.isControlDown())
					{
						int rotation = Mobile.rotateDisplay + 90;
						if(rotation == 360) { rotation = 0; }
						Mobile.config.settings.put("rotate",  "" + rotation);
						settingsChanged();
					}
				break;
				case KeyEvent.VK_V: // Toggle Virtual Keypad (Ctrl+Alt+V)
					if(e.isAltDown() && e.isControlDown())
					{
						awtGUI.toggleVirtualKeypad();
					}
				break;
			}

			// Forward printable text keys (letters, digits, punctuation, backspace, space)
			// straight into the MIDlet as ASCII keycodes, so the physical PC keyboard
			// can be used for text input (email/password/chat fields).
			boolean shiftDown = (e.getModifiersEx() & java.awt.event.InputEvent.SHIFT_DOWN_MASK) != 0;
			char keyChar = e.getKeyChar();
			if(!e.isAltDown() && !e.isControlDown() && !e.isMetaDown() && isTextKey(keycode, keyChar))
			{
				int textKey = getTextKeycode(keycode, keyChar, shiftDown);
				if(textKey > 0 && textKey < 128)
				{
					if(!textKeysPressed[textKey])
					{
						textKeysPressed[textKey] = true;
						MobilePlatform.keyPressed(textKey);
					}
					return;
				}
			}

			if (mobikey == Integer.MIN_VALUE) // Ignore events from keys not mapped to a phone keypad key (AWTGUI does use 0, so this can't mirror libretro)
			{
				return;
			}

			if (MobilePlatform.pressedKeys[mobikey] == false)
			{
				if(mobikey < 20) // Anything over 20 are special keys (fast-forward, etc)
				{
					MobilePlatform.pressedKeys[mobikey] = true;
					MobilePlatform.keyPressed(Mobile.getMobileKey(mobikey));
				}
				else
				{
					if((e.isAltDown() && e.isControlDown()) || ignoreModifiers)
					{
						MobilePlatform.pressedKeys[mobikey] = true;
					}
				}
			}
			else
			{
				if(mobikey < 20) { MobilePlatform.keyRepeated(Mobile.getMobileKey(mobikey)); }
			}
		}
	}

	protected void releaseKey(KeyEvent e)
	{
		if(awtGUI.hasLoadedFile())
		{
			int mobikey = getMobileKey(e.getKeyCode());

			// Release forwarded text keys
			boolean shiftDown = (e.getModifiersEx() & java.awt.event.InputEvent.SHIFT_DOWN_MASK) != 0;
			char keyChar = e.getKeyChar();
			if(!e.isAltDown() && !e.isControlDown() && !e.isMetaDown() && isTextKey(e.getKeyCode(), keyChar))
			{
				int textKey = getTextKeycode(e.getKeyCode(), keyChar, shiftDown);
				if(textKey > 0 && textKey < 128)
				{
					if(textKeysPressed[textKey])
					{
						textKeysPressed[textKey] = false;
						MobilePlatform.keyReleased(textKey);
					}
					return;
				}
			}

			if (mobikey == Integer.MIN_VALUE) // Ignore events from keys not mapped to a phone keypad key (AWTGUI does use 0, so this can't mirror libretro)
			{
				return;
			}

			// Figures we must only release if the key is pressed. This vastly simplifies external input event handling
			if(MobilePlatform.pressedKeys[mobikey])
			{
				MobilePlatform.pressedKeys[mobikey] = false;
				MobilePlatform.keyReleased(Mobile.getMobileKey(mobikey));

				if(mobikey == 21) { ScreenShot.takeScreenshot(false); }
				else if(mobikey == 22) { MobilePlatform.pauseResumeApp(); }

				for(int i = 0; i < MobilePlatform.pressedKeys.length; i++)
				{
					if(MobilePlatform.pressedKeys[i]) { MobilePlatform.keyRepeated(Mobile.getMobileKey(i)); }
				}
			}
		}
	}

	private static String getFormattedLocation(String loc)
	{
		if (loc.startsWith("file://") || loc.startsWith("http://") || loc.startsWith("https://"))
			return loc;

		File file = new File(loc);
		if(!file.isFile())
		{
			Mobile.log(Mobile.LOG_ERROR, FreeJ2ME.class.getPackage().getName() + "." + FreeJ2ME.class.getSimpleName() + ": " + "File not found...");
			System.exit(0);
		}

		return file.toURI().toString();
	}

	private void settingsChanged()
	{
		boolean hasRotated = Mobile.updateSettings();

		// Create a standard size LCD if not rotated, else invert window's width and height.
		if(Mobile.lcdWidth != lcdWidth || Mobile.lcdHeight != lcdHeight || hasRotated)
		{
			Mobile.getPlatform().resizeLCD(Mobile.lcdWidth, Mobile.lcdHeight);

			if(Mobile.rotateDisplay == 0 || Mobile.rotateDisplay == 180)
			{
				lcdWidth = Mobile.lcdWidth;
				lcdHeight = Mobile.lcdHeight;
			}
			else
			{
				lcdWidth = Mobile.lcdHeight;
				lcdHeight = Mobile.lcdWidth;
			}
			resize();
			if(!isFullscreen) { main.setSize(lcdWidth*scaleFactor+xborder , lcdHeight*scaleFactor+yborder); }
			lcd.clearScreen();
		}

		awtGUI.updateOptions();
	}

	private int getMobileKey(int keycode)
	{
		for(int i = 0; i < awtGUI.inputKeycodes.length; i++)
		{
			if(keycode == awtGUI.inputKeycodes[i]) { return Mobile.convertAWTKeycode(i);}
		}
		return Integer.MIN_VALUE;
	}

	// Distinguishes physical-keyboard text keys (letters, digits, punctuation, space, backspace)
	// from game-control keys (arrows, numpad, enter, tab, function keys, modifiers).
	private boolean isTextKey(int keycode, char keyChar)
	{
		if(keycode >= KeyEvent.VK_NUMPAD0 && keycode <= KeyEvent.VK_NUMPAD9) { return false; }
		if(keycode >= KeyEvent.VK_F1 && keycode <= KeyEvent.VK_F12) { return false; }
		switch(keycode)
		{
			case KeyEvent.VK_UP:
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_ENTER:
			case KeyEvent.VK_TAB:
			case KeyEvent.VK_ESCAPE:
			case KeyEvent.VK_SHIFT:
			case KeyEvent.VK_CONTROL:
			case KeyEvent.VK_ALT:
			case KeyEvent.VK_META:
			case KeyEvent.VK_CAPS_LOCK:
			case KeyEvent.VK_DELETE:
			case KeyEvent.VK_HOME:
			case KeyEvent.VK_END:
			case KeyEvent.VK_PAGE_UP:
			case KeyEvent.VK_PAGE_DOWN:
			case KeyEvent.VK_INSERT:
				return false;
		}
		if(keycode >= KeyEvent.VK_A && keycode <= KeyEvent.VK_Z) { return true; }
		if(keycode >= KeyEvent.VK_0 && keycode <= KeyEvent.VK_9) { return true; }
		if(keyChar != KeyEvent.CHAR_UNDEFINED && keyChar >= 32 && keyChar < 127) { return true; }
		switch(keycode)
		{
			case KeyEvent.VK_BACK_SPACE:
			case KeyEvent.VK_SPACE:
			case KeyEvent.VK_PERIOD:
			case KeyEvent.VK_COMMA:
			case KeyEvent.VK_SLASH:
			case KeyEvent.VK_MINUS:
			case KeyEvent.VK_EQUALS:
			case KeyEvent.VK_SEMICOLON:
			case KeyEvent.VK_QUOTE:
			case KeyEvent.VK_OPEN_BRACKET:
			case KeyEvent.VK_CLOSE_BRACKET:
			case KeyEvent.VK_BACK_SLASH:
			case KeyEvent.VK_BACK_QUOTE:
				return true;
		}
		return false;
	}

	// Resolves a physical-keyboard key to the ASCII keycode that J2ME Canvas expects.
	// Prefers KeyEvent's character when available, otherwise falls back to mapping
	// the AWT keycode (letter/digit/punctuation) while honoring the Shift modifier.
	private int getTextKeycode(int keycode, char keyChar, boolean shiftDown)
	{
		if(keyChar != KeyEvent.CHAR_UNDEFINED && keyChar >= 32 && keyChar < 127) { return keyChar; }
		boolean capsLock = false;
		try { capsLock = Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK); }
		catch(Exception ex) { }
		boolean upper = shiftDown ^ capsLock;
		switch(keycode)
		{
			case KeyEvent.VK_SPACE: return 32;
			case KeyEvent.VK_BACK_SPACE: return 8;
			case KeyEvent.VK_PERIOD: return upper ? 62 : 46;
			case KeyEvent.VK_COMMA: return upper ? 60 : 44;
			case KeyEvent.VK_SLASH: return upper ? 63 : 47;
			case KeyEvent.VK_MINUS: return upper ? 95 : 45;
			case KeyEvent.VK_EQUALS: return upper ? 43 : 61;
			case KeyEvent.VK_SEMICOLON: return upper ? 58 : 59;
			case KeyEvent.VK_QUOTE: return upper ? 34 : 39;
			case KeyEvent.VK_OPEN_BRACKET: return upper ? 123 : 91;
			case KeyEvent.VK_CLOSE_BRACKET: return upper ? 125 : 93;
			case KeyEvent.VK_BACK_SLASH: return upper ? 124 : 92;
			case KeyEvent.VK_BACK_QUOTE: return upper ? 126 : 96;
		}
		if(keycode >= KeyEvent.VK_A && keycode <= KeyEvent.VK_Z)
		{
			return upper ? (int) ('A' + (keycode - KeyEvent.VK_A)) : (int) ('a' + (keycode - KeyEvent.VK_A));
		}
		if(keycode >= KeyEvent.VK_0 && keycode <= KeyEvent.VK_9)
		{
			char digit = (char) ('0' + (keycode - KeyEvent.VK_0));
			if(!shiftDown) { return digit; }
			switch(digit)
			{
				case '1': return 33;
				case '2': return 64;
				case '3': return 35;
				case '4': return 36;
				case '5': return 37;
				case '6': return 94;
				case '7': return 38;
				case '8': return 42;
				case '9': return 40;
				case '0': return 41;
			}
		}
		return -1;
	}

	private void resize()
	{
		xborder = main.getInsets().left+main.getInsets().right;
		yborder = main.getInsets().top+main.getInsets().bottom;

		double vw = (main.getWidth()-xborder)*1;
		double vh = (main.getHeight()-yborder)*1;

		double nw = lcdWidth;
		double nh = lcdHeight;

		nw = vw;
		nh = nw*((double)lcdHeight/(double)lcdWidth);

		if(nh>vh)
		{
			nh = vh;
			nw = nh*((double)lcdWidth/(double)lcdHeight);
		}

		lcd.updateScale((int)nw, (int)nh);
	}

	public void toggleFullscreen()
	{
        isFullscreen = !isFullscreen;
		main.dispose();
		constructFreeJ2MEGUI();
		displayGUI();
    }

	private void constructFreeJ2MEGUI()
	{
		main = new Frame("FreeJ2ME-Plus");

		if (isFullscreen)
		{
            main.setUndecorated(true);
            main.setSize(Toolkit.getDefaultToolkit().getScreenSize());
        }
		else
		{
            main.setSize(350, 450);
            main.setMinimumSize(new Dimension(192, 64));
			main.setLocationRelativeTo(null); // Center window on screen
        }

		main.setBackground(Color.BLACK);

		try
		{
			main.setIconImage(ImageIO.read(main.getClass().getResourceAsStream("/org/recompile/icon.png")));
		}
		catch (Exception e) { }

		main.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				System.exit(0);
			}
		});

		/* Add LCD screen to FreeJ2ME's AWT frame */
		main.add(lcd);

		awtGUI.setMainFrame(main);
		/* Append the awt menu bar into FreeJ2ME's frame */
		if(!isFullscreen) { main.setMenuBar(awtGUI.getMenuBar()); }
	}

	private void displayGUI()
	{
		main.addComponentListener(new ComponentAdapter()
		{
			public void componentResized(ComponentEvent e) { resize(); }
		});

		main.setVisible(true);
		main.pack();
		resize();
		if(!isFullscreen) { main.setSize(lcdWidth*scaleFactor+xborder, lcdHeight*scaleFactor+yborder); }

		awtGUI.updateDialogs();
	}

	private class LCD extends Canvas
	{
		private boolean showDragMessage = false, fileSupported = false;
		public int cx=0;
		public int cy=0;
		public int cw=240;
		public int ch=320;

		public double scalex=1;
		public double scaley=1;

		public LCD()
		{
			setDropTarget();
			setBackground(Color.WHITE);
		}

		public void updateScale(int vw, int vh)
		{
			cx = (this.getWidth()-vw)/2;
			cy = (this.getHeight()-vh)/2;
			cw = vw;
			ch = vh;
			scalex = (double)lcdWidth/(double)vw;
			scaley = (double)lcdHeight/(double)vh;
		}

		@Override
        public void update(Graphics g) { paint(g); }

		// Used to clear the entire framebuffer when rotated in fullscreen to remove garbage pixels
		public void clearScreen()
		{
			((Graphics2D) this.getGraphics()).clearRect(0, 0, getWidth(), getHeight());
		}

		public void paint(Graphics g)
		{
			/* Only update mem dialog's stats and console window if they are visible */
			if(awtGUI.awtDialogs[2].isVisible()) { awtGUI.updateDialogs(); }

			if(!showDragMessage)
			{
				if(!awtGUI.hasLoadedFile())
				{
					// Draw FreeJ2ME-Plus' intro screen
					g.setColor(Color.BLACK);
					g.fillRect(0, 0, getWidth(), getHeight());

					g.drawImage(main.getIconImage(),
						getWidth()/2 - main.getIconImage().getWidth(null)/2,
						getHeight()/2 - main.getIconImage().getHeight(null)/2,
						Math.min(getWidth(), main.getIconImage().getWidth(null)),
						Math.min(getHeight(), main.getIconImage().getHeight(null)),
						null);

					g.setColor(new Color(55, 55, 125, 238));
					g.fillRect(0, 0, getWidth(), getHeight());

					g.setColor(Color.ORANGE);
					g.setFont(new Font("Dialog", Font.BOLD | Font.ITALIC, 24));
					String text = "FreeJ2ME-Plus V1.53";
					FontMetrics metrics = g.getFontMetrics();
					g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2,
						(getHeight() / 2) - metrics.getHeight());

					g.setFont(new Font("Dialog", Font.BOLD, 16));
					text = "Please use the 'File' menu";
					metrics = g.getFontMetrics();
					g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2,
						(getHeight() / 2));

					text = "or drop a valid J2ME app";
					metrics = g.getFontMetrics();
					g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2,
						(getHeight() / 2) + metrics.getHeight());

					text = "inside this window.";
					metrics = g.getFontMetrics();
					g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2,
						(getHeight() / 2) + metrics.getHeight() * 2);

					return;
				}
				// Draw Pause or Fast-Forward indicators before showing the frame on screen
				if(Mobile.isPaused)
				{
					((PlatformGraphics)Mobile.getPlatform().getLcdFrontbufferGraphics()).drawPauseIndicator();
				}
				else if (MobilePlatform.pressedKeys[20]) // Check if fast-forward is active
				{
					((PlatformGraphics)Mobile.getPlatform().getLcdFrontbufferGraphics()).drawFastForwardIndicator();
				}

				if (Mobile.rotateDisplay == 0) { g.drawImage(Mobile.getPlatform().getLcdFrontbuffer().getCanvas(), cx, cy, cw, ch, null); }
				else
				{
					if(Mobile.rotateDisplay == 90)
					{
						((Graphics2D) g).rotate(Math.toRadians(90), cw/2, cw/2);
						g.drawImage(Mobile.getPlatform().getLcdFrontbuffer().getCanvas(), 0, cx, ch, cw, null);
					}
					else if(Mobile.rotateDisplay == 180)
					{
						((Graphics2D) g).rotate(Math.toRadians(180), cw/2, ch/2);
        				g.drawImage(Mobile.getPlatform().getLcdFrontbuffer().getCanvas(), -cx, cy, cw, ch, null);
					}
					else if(Mobile.rotateDisplay == 270)
					{
						((Graphics2D) g).rotate(Math.toRadians(270), ch/2, ch/2);
						g.drawImage(Mobile.getPlatform().getLcdFrontbuffer().getCanvas(), 0, cx, ch, cw, null);
					}
				}
			}
			else
			{
				g.setColor(freeJ2MEDragColor);
				g.fillRect(cx, cy, cw, ch);
				g.setFont(new Font("Dialog", Font.BOLD, 20));
				g.setColor(fileSupported ? Color.ORANGE : Color.RED);
				String message = fileSupported ? ">> DROP HERE <<" : "INVALID FILE TYPE!!!";
				FontMetrics metrics = g.getFontMetrics();
				int x = (getWidth() - metrics.stringWidth(message)) / 2;
				int y = (getHeight() / 2);
				g.drawString(message, x, y);
			}
		}

		private void setDropTarget()
		{
			new DropTarget(this, new DropTargetListener()
			{
				@Override
				@SuppressWarnings("unchecked")
				public void dragEnter(DropTargetDragEvent dtde)
				{
					try
					{
						if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
						{
							// Get the files being dragged
							Transferable transferable = dtde.getTransferable();
							java.util.List<File> files = (java.util.List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

							// Check if the file is supported
							for (File file : files)
							{
								if (isSupportedFile(file.getName()))
								{
									dtde.acceptDrag(DnDConstants.ACTION_COPY);
									fileSupported = true;
									break;
								}
								else
								{
									dtde.rejectDrag();
									fileSupported = false;
								}
							}
						}
						else { dtde.rejectDrag(); }
					} catch (Exception e) { e.printStackTrace(); }

					showDragMessage = true;
					repaint();
				}

				@Override
				public void dragOver(DropTargetDragEvent dtde) { }

				@Override
				public void dropActionChanged(DropTargetDragEvent dtde) { }

				@Override
				public void dragExit(DropTargetEvent dte)
				{
					showDragMessage = false;
					repaint();
				}

				@Override
				@SuppressWarnings("unchecked")
				public void drop(DropTargetDropEvent dtde)
				{
					try
					{
						dtde.acceptDrop(DnDConstants.ACTION_COPY);
						Transferable transferable = dtde.getTransferable();
						if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
						{
							java.util.List<File> files = (java.util.List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
							if (!files.isEmpty() && fileSupported)
							{
								// Load the dropped file
								if(!awtGUI.hasLoadedFile()) { awtGUI.loadJarFile(files.get(0).toURI().toString()); }
								else // Ask for a restart if a jar is already running
								{
									MobilePlatform.fileName = files.get(0).toURI().toString();
									awtGUI.showRestartDialog();
								}
							}
						}
					}
					catch (Exception e) { System.out.println("Exception caught in Drag and Drop:" + e.getMessage()); }
					finally
					{
						dtde.dropComplete(true);
						showDragMessage = false;
						repaint();
					}
				}
			});
		}

		private boolean isSupportedFile(String fileName)
		{
			// Check for supported extensions with drag and drop
			return fileName.toLowerCase().endsWith(".jar") ||
				   fileName.toLowerCase().endsWith(".jad") ||
				   fileName.toLowerCase().endsWith(".kjx");
		}
	}
}
