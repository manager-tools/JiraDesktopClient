/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:  
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "SOAP" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 2000, International
 * Business Machines, Inc., http://www.apache.org.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.soap.util.net;

import java.net.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A <code>TcpTunnelGui</code> object listens on the given port,
 * and once <code>Start</code> is pressed, will forward all bytes
 * to the given host and port. All traffic is displayed in a
 * UI.
 *
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 */
public class TcpTunnelGui extends Frame {
  int listenPort;
  String tunnelHost;
  int tunnelPort;
  TextArea listenText, tunnelText;
  Label status;
  Relay inRelay, outRelay;

  public TcpTunnelGui (int listenPort, String tunnelHost, int tunnelPort) {
    Panel p;
    
    this.listenPort = listenPort;
    this.tunnelHost = tunnelHost;
    this.tunnelPort = tunnelPort;

    addWindowListener (new WindowAdapter () {
      public void windowClosing (WindowEvent e) {
        System.exit (0);
      }
    });

    // show info
    setTitle ("TCP Tunnel/Monitor: Tunneling localhost:" + listenPort +
              " to " + tunnelHost + ":" + tunnelPort);

    // labels
    p = new Panel ();
    p.setLayout (new BorderLayout ());
    Label l1, l2;
    p.add ("West",
           l1 = new Label ("From localhost:" + listenPort, Label.CENTER));
    p.add ("East",
           l2 = new Label ("From " + tunnelHost + ":" + tunnelPort,
                           Label.CENTER));
    add ("North", p);

    // the monitor part
    p = new Panel ();
    p.setLayout (new GridLayout (-1,2));
    p.add (listenText = new TextArea ());
    p.add (tunnelText = new TextArea ());
    add ("Center", p);

    // clear and status
    Panel p2 = new Panel ();
    p2.setLayout (new BorderLayout ());

    p = new Panel ();
    Button b = new Button ("Clear");
    b.addActionListener (new ActionListener () {
      public void actionPerformed (ActionEvent e) {
        listenText.setText ("");
        tunnelText.setText ("");
      }
    });
    p.add (b);
    p2.add ("Center", p);

    p2.add ("South", status = new Label ());
    add ("South", p2);

    pack ();
    show ();

    Font f = l1.getFont ();
    l1.setFont (new Font (f.getName (), Font.BOLD, f.getSize ()));
    l2.setFont (new Font (f.getName (), Font.BOLD, f.getSize ()));
  }

  public int getListenPort () {
    return listenPort;
  }

  public String getTunnelHost () {
    return tunnelHost;
  }

  public int getTunnelPort () {
    return tunnelPort;
  }
  
  public TextArea getListenText () {
    return listenText;
  }

  public TextArea getTunnelText () {
    return tunnelText;
  }

  public Label getStatus () {
    return status;
  }

  public static void main (String args[]) throws IOException {
    if (args.length != 3) {
      System.err.println ("Usage: java TcpTunnelGui listenport tunnelhost " +
                          "tunnelport");
      System.exit (1);
    }
    
    int listenPort = Integer.parseInt (args[0]);
    String tunnelHost = args[1];
    int tunnelPort = Integer.parseInt (args[2]);
    final TcpTunnelGui ttg = 
      new TcpTunnelGui (listenPort, tunnelHost, tunnelPort);

    // create the server thread
    Thread server = new Thread () {
      public void run () {
        ServerSocket ss = null;
        Label status = ttg.getStatus ();
        try {
          ss = new ServerSocket (ttg.getListenPort ());
        } catch (Exception e) {
          e.printStackTrace ();
          System.exit (1);
        }
        while (true) {
          try {
            status.setText ("Listening for connections on port " + 
                            ttg.getListenPort () + " ...");
            // accept the connection from my client
            Socket sc = ss.accept ();
            
            // connect to the thing I'm tunnelling for
            Socket st = new Socket (ttg.getTunnelHost (),
                                    ttg.getTunnelPort ());
            
            status.setText ("Tunnelling port " + ttg.getListenPort () +
                            " to port " + ttg.getTunnelPort () + 
                            " on host " + ttg.getTunnelHost () + " ...");
            
            // relay the stuff thru
            new Relay (sc.getInputStream (), st.getOutputStream (),
                       ttg.getListenText ()).start ();
            new Relay (st.getInputStream (), sc.getOutputStream (),
                       ttg.getTunnelText ()).start ();
            
            // that's it .. they're off; now I go back to my stuff.
          } catch (Exception ee) {
            status.setText ("Ouch! [See console for details]: " + 
                            ee.getMessage ());
            ee.printStackTrace ();
          }
        }
      }
    };
    server.start ();
  }
}
