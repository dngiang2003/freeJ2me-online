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

import java.awt.Button;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;

public final class VirtualKeyboard extends Dialog
{
	private final Button[] keyButtons = new Button[23];
	private final Button copyButton = new Button("Copy");
	private final Button pasteButton = new Button("Paste");

	// Full text keyboard layout (QWERTY). Each char is the base (unshifted) value.
	private static final char[][] TEXT_LAYOUT =
	{
		{ 'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p' },
		{ 'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ';' },
		{ 'z', 'x', 'c', 'v', 'b', 'n', 'm', ',', '.', '/' },
		{ '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' }
	};

	// Special text keys: label, base char, shifted char, backspace marker.
	private static final String[] SPECIAL_LABELS = { "Space", "@", "_", "-", "+", "Back" };
	private static final int[] SPECIAL_CODES   = { 32,      64,  95,  45,  43,  8    };

	private boolean shiftOn = false;
	private Button shiftButton = new Button("Shift");

	private final String[] keyLabels =
	{
		"LSoft", "RSoft",
		"Up", "Left", "Fire", "Right", "Down",
		"7", "8", "9", "4", "5", "6", "1", "2", "3",
		"*", "0", "#", "CLR",
		"FF", "Shot", "Pause"
	};

	public VirtualKeyboard(Frame parent)
	{
		super(parent, "Virtual Keypad", false);
		setupKeyboard();
	}

	private void setupKeyboard()
	{
		setBackground(FreeJ2ME.freeJ2MEBGColor);
		setForeground(Color.ORANGE);
		setResizable(false);

		Panel gamePanel = new Panel(new GridLayout(0, 3, 3, 3));
		gamePanel.setBackground(FreeJ2ME.freeJ2MEBGColor);

		for (int i = 0; i < keyButtons.length; i++)
		{
			final int index = i;
			keyButtons[i] = new Button(keyLabels[i]);
			keyButtons[i].setBackground(FreeJ2ME.freeJ2MEDragColor);
			keyButtons[i].setForeground(Color.ORANGE);
			keyButtons[i].addMouseListener(new MouseAdapter()
			{
				public void mousePressed(MouseEvent e) { keyPress(index); }
				public void mouseReleased(MouseEvent e) { keyRelease(index); }
			});
		}

		copyButton.setBackground(FreeJ2ME.freeJ2MEDragColor);
		copyButton.setForeground(Color.GREEN);
		pasteButton.setBackground(FreeJ2ME.freeJ2MEDragColor);
		pasteButton.setForeground(Color.GREEN);

		copyButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) { copyToClipboard(); }
		});

		pasteButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) { pasteFromClipboard(); }
		});

		// Soft keys + D-Pad
		gamePanel.add(keyButtons[0]);  // LSoft
		gamePanel.add(keyButtons[2]);  // Up
		gamePanel.add(keyButtons[1]);  // RSoft

		gamePanel.add(keyButtons[3]);  // Left
		gamePanel.add(keyButtons[4]);  // Fire
		gamePanel.add(keyButtons[5]);  // Right

		gamePanel.add(keyButtons[20]); // Fast-Forward
		gamePanel.add(keyButtons[6]);  // Down
		gamePanel.add(keyButtons[22]); // Pause

		// Number keypad
		gamePanel.add(keyButtons[7]);  // 7
		gamePanel.add(keyButtons[8]);  // 8
		gamePanel.add(keyButtons[9]);  // 9

		gamePanel.add(keyButtons[10]); // 4
		gamePanel.add(keyButtons[11]); // 5
		gamePanel.add(keyButtons[12]); // 6

		gamePanel.add(keyButtons[13]); // 1
		gamePanel.add(keyButtons[14]); // 2
		gamePanel.add(keyButtons[15]); // 3

		gamePanel.add(keyButtons[16]); // *
		gamePanel.add(keyButtons[17]); // 0
		gamePanel.add(keyButtons[18]); // #

		// Clipboard helpers
		gamePanel.add(copyButton);
		gamePanel.add(keyButtons[19]); // CLR
		gamePanel.add(pasteButton);

		// ---- Full text keyboard (QWERTY) ----
		Panel textPanel = new Panel(new GridLayout(0, 10, 2, 2));
		textPanel.setBackground(FreeJ2ME.freeJ2MEBGColor);

		for (char[] row : TEXT_LAYOUT)
		{
			for (char c : row)
			{
				final char base = c;
				final Button b = new Button(String.valueOf(base));
				styleTextButton(b);
				b.addMouseListener(new MouseAdapter()
				{
					public void mousePressed(MouseEvent e)
					{
						char out = shiftOn ? shiftChar(base) : base;
						textKeyPress(out);
					}
					public void mouseReleased(MouseEvent e)
					{
						char out = shiftOn ? shiftChar(base) : base;
						textKeyRelease(out);
					}
				});
				textPanel.add(b);
			}
		}

		for (int i = 0; i < SPECIAL_LABELS.length; i++)
		{
			final int code = SPECIAL_CODES[i];
			final Button b = new Button(SPECIAL_LABELS[i]);
			styleTextButton(b);
			b.addMouseListener(new MouseAdapter()
			{
				public void mousePressed(MouseEvent e) { textKeyPress((char) code); }
				public void mouseReleased(MouseEvent e) { textKeyRelease((char) code); }
			});
			textPanel.add(b);
		}

		// Shift toggle
		shiftButton.setBackground(FreeJ2ME.freeJ2MEDragColor);
		shiftButton.setForeground(Color.CYAN);
		shiftButton.setFont(new Font("Monospaced", Font.BOLD, 11));
		shiftButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e) { shiftOn = !shiftOn; }
		});
		textPanel.add(shiftButton);

		setLayout(new FlowLayout());
		add(gamePanel);
		add(textPanel);

		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e) { setVisible(false); }
		});

		setSize(330, 480);
	}

	private void styleTextButton(Button b)
	{
		b.setBackground(FreeJ2ME.freeJ2MEDragColor);
		b.setForeground(Color.ORANGE);
		b.setFont(new Font("Monospaced", Font.BOLD, 11));
	}

	// Maps a base char to its Shifted counterpart (US layout).
	private char shiftChar(char c)
	{
		if (c >= 'a' && c <= 'z') { return Character.toUpperCase(c); }
		switch (c)
		{
			case '0': return ')';
			case '1': return '!';
			case '2': return '@';
			case '3': return '#';
			case '4': return '$';
			case '5': return '%';
			case '6': return '^';
			case '7': return '&';
			case '8': return '*';
			case '9': return '(';
			case ';': return ':';
			case ',': return '<';
			case '.': return '>';
			case '/': return '?';
		}
		return c;
	}

	private void keyPress(int index)
	{
		if (FreeJ2ME.app == null) { return; }
		int keycode = AWTGUI.inputKeycodes[index];
		FreeJ2ME.app.pressKey(new KeyEvent(FreeJ2ME.app.main, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, keycode, KeyEvent.CHAR_UNDEFINED), true);
	}

	private void keyRelease(int index)
	{
		if (FreeJ2ME.app == null) { return; }
		int keycode = AWTGUI.inputKeycodes[index];
		FreeJ2ME.app.releaseKey(new KeyEvent(FreeJ2ME.app.main, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, keycode, KeyEvent.CHAR_UNDEFINED));
	}

	// Sends a text key straight to the MIDlet as an ASCII keycode.
	private void textKeyPress(char c)
	{
		if (FreeJ2ME.app == null) { return; }
		MobilePlatform.keyPressed((int) c);
	}

	private void textKeyRelease(char c)
	{
		if (FreeJ2ME.app == null) { return; }
		MobilePlatform.keyReleased((int) c);
	}

	private void copyToClipboard()
	{
		String text = MobilePlatform.getFocusedText();
		if (text == null || text.length() == 0)
		{
			Mobile.log(Mobile.LOG_WARNING, VirtualKeyboard.class.getPackage().getName() + "." + VirtualKeyboard.class.getSimpleName() + ": " + "No focused text field to copy from");
			return;
		}
		try
		{
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
			Mobile.log(Mobile.LOG_INFO, VirtualKeyboard.class.getPackage().getName() + "." + VirtualKeyboard.class.getSimpleName() + ": " + "Copied text field contents to clipboard");
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_WARNING, VirtualKeyboard.class.getPackage().getName() + "." + VirtualKeyboard.class.getSimpleName() + ": " + "Couldn't copy to clipboard: " + e.getMessage());
		}
	}

	private void pasteFromClipboard()
	{
		try
		{
			String data = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
			if (data == null || data.length() == 0)
			{
				Mobile.log(Mobile.LOG_WARNING, VirtualKeyboard.class.getPackage().getName() + "." + VirtualKeyboard.class.getSimpleName() + ": " + "Clipboard is empty");
				return;
			}
			MobilePlatform.pasteText(data);
			Mobile.log(Mobile.LOG_INFO, VirtualKeyboard.class.getPackage().getName() + "." + VirtualKeyboard.class.getSimpleName() + ": " + "Pasted clipboard contents into focused text field");
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_WARNING, VirtualKeyboard.class.getPackage().getName() + "." + VirtualKeyboard.class.getSimpleName() + ": " + "Couldn't read clipboard: " + e.getMessage());
		}
	}

	public void followMainWindow()
	{
		if (FreeJ2ME.app == null || FreeJ2ME.app.main == null) { return; }
		setLocation(FreeJ2ME.app.main.getLocation().x + FreeJ2ME.app.main.getSize().width, FreeJ2ME.app.main.getLocation().y);
	}
}
