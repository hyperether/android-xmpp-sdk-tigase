/*
 * XMPPService.java
 *
 * Tigase Android Messenger
 * Copyright (C) 2011-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */

package org.tigase.messenger.phone.pro.service;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.SSLCertificateSocketFactory;
import android.net.SSLSessionCache;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.hyperether.toolbox.HyperLog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.tigase.messenger.TigaseSdk;
import org.tigase.messenger.events.DownloadFileEvent;
import org.tigase.messenger.events.DownloadedFileEvent;
import org.tigase.messenger.events.UploadedFileEvent;
import org.tigase.messenger.interfaces.Constants;
import org.tigase.messenger.interfaces.DataCache;
import org.tigase.messenger.interfaces.NotificationHandler;
import org.tigase.messenger.interfaces.SharedPrefs;
import org.tigase.messenger.jaxmpp.android.caps.CapabilitiesDBCache;
import org.tigase.messenger.jaxmpp.android.chat.AndroidChatManager;
import org.tigase.messenger.jaxmpp.android.chat.MarkAsRead;
import org.tigase.messenger.jaxmpp.android.muc.AndroidRoomsManager;
import org.tigase.messenger.jaxmpp.android.roster.AndroidRosterStore;
import org.tigase.messenger.phone.pro.account.AccountsConstants;
import org.tigase.messenger.phone.pro.account.PrioritiesEntity;
import org.tigase.messenger.phone.pro.db.CPresence;
import org.tigase.messenger.phone.pro.db.DatabaseContract;
import org.tigase.messenger.phone.pro.db.DatabaseHelper;
import org.tigase.messenger.phone.pro.db.RosterProviderExt;
import org.tigase.messenger.phone.pro.utils.AccountHelper;
import org.tigase.messenger.phone.pro.utils.Crypto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocketFactory;

import messenger.tigase.org.tigasesdk.R;
import tigase.jaxmpp.android.Jaxmpp;
import tigase.jaxmpp.core.client.AsyncCallback;
import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.Connector;
import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.JaxmppCore;
import tigase.jaxmpp.core.client.MultiJaxmpp;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.ElementBuilder;
import tigase.jaxmpp.core.client.xml.ElementFactory;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.forms.JabberDataElement;
import tigase.jaxmpp.core.client.xmpp.modules.EntityTimeModule;
import tigase.jaxmpp.core.client.xmpp.modules.PingModule;
import tigase.jaxmpp.core.client.xmpp.modules.SoftwareVersionModule;
import tigase.jaxmpp.core.client.xmpp.modules.adhoc.AdHocCommansModule;
import tigase.jaxmpp.core.client.xmpp.modules.auth.AuthModule;
import tigase.jaxmpp.core.client.xmpp.modules.auth.SaslModule;
import tigase.jaxmpp.core.client.xmpp.modules.capabilities.CapabilitiesModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.Chat;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageCarbonsModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.xep0085.ChatState;
import tigase.jaxmpp.core.client.xmpp.modules.chat.xep0085.ChatStateExtension;
import tigase.jaxmpp.core.client.xmpp.modules.disco.DiscoveryModule;
import tigase.jaxmpp.core.client.xmpp.modules.httpfileupload.HttpFileUploadModule;
import tigase.jaxmpp.core.client.xmpp.modules.mam.MessageArchiveManagementModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Affiliation;
import tigase.jaxmpp.core.client.xmpp.modules.muc.MucModule;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Occupant;
import tigase.jaxmpp.core.client.xmpp.modules.muc.Room;
import tigase.jaxmpp.core.client.xmpp.modules.muc.XMucUserElement;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceStore;
import tigase.jaxmpp.core.client.xmpp.modules.push.PushNotificationModule;
import tigase.jaxmpp.core.client.xmpp.modules.roster.RosterModule;
import tigase.jaxmpp.core.client.xmpp.modules.streammng.StreamManagementModule;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCard;
import tigase.jaxmpp.core.client.xmpp.modules.vcard.VCardModule;
import tigase.jaxmpp.core.client.xmpp.modules.xep0136.MessageArchivingModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.ErrorElement;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.Message;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence;
import tigase.jaxmpp.core.client.xmpp.stanzas.Stanza;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;
import tigase.jaxmpp.core.client.xmpp.utils.delay.XmppDelay;
import tigase.jaxmpp.j2se.J2SEPresenceStore;
import tigase.jaxmpp.j2se.J2SESessionObject;
import tigase.jaxmpp.j2se.connectors.socket.SocketConnector;

public class XMPPService
        extends Service {

    public static final String CLIENT_PRESENCE_CHANGED_ACTION = "org.tigase.messenger.phone.pro.PRESENCE_CHANGED";

    public static final String CUSTOM_PRIORITIES_ENTITY_KEY = "CUSTOM_PRIORITIES_ENTITY_KEY";

    public static final String CONNECT_ALL = "connect-all";

    public static final String CONNECT_SINGLE = "connect-single";
    public static final String ACCOUNT_TMP_DISABLED_KEY = "ACC:DISABLED";
    public static final String LAST_ACCOUNT_ACTIVITY = "org.tigase.messenger.phone.pro.service.XMPPService.LAST_ACCOUNT_ACTIVITY";
    public static final String SEND_FILE_ACTION = "org.tigase.messenger.phone.pro.service.XMPPService.SEND_FILE_ACTION";
    final static String TAG = "XMPPService";
    private static final String ON_CONNECT_RUNNABLE_ARRAY_KEY = "ON_CONNECT_RUNNABLE_ARRAY";
    private static final String KEEPALIVE_ACTION = "org.tigase.messenger.phone.pro.service.XMPPService.KEEP_ALIVE";
    private static final StanzaExecutor executor = new StanzaExecutor();
    private static final String ACCOUNT_ID = "ID";

    public static enum DisconnectionCauses {
        CERTIFICATE_ERROR,
        AUTHENTICATION
    }

    protected final Timer timer = new Timer();
    final MultiJaxmpp multiJaxmpp = new MultiJaxmpp();
    final ScreenStateReceiver screenStateReceiver = new ScreenStateReceiver();
    private final AutopresenceManager autopresenceManager = new AutopresenceManager(this);
    private final IBinder mBinder = new LocalBinder();
    private final DiscoveryModule.ServerFeaturesReceivedHandler streamHandler = new DiscoveryModule.ServerFeaturesReceivedHandler() {

        @Override
        public void onServerFeaturesReceived(final SessionObject sessionObject, IQ stanza,
                                             String[] featuresArr) {
            Set<String> features = new HashSet<String>(Arrays.asList(featuresArr));
            if (features.contains(MessageCarbonsModule.XMLNS_MC)) {
                MessageCarbonsModule mc = multiJaxmpp.get(sessionObject)
                        .getModule(MessageCarbonsModule.class);
                // if we decide to disable MessageCarbons for some account we
                // may not create module
                // instance at all, so better be prepared for null here
                if (mc != null) {
                    try {
                        mc.enable(new AsyncCallback() {
                            @Override
                            public void onError(Stanza responseStanza,
                                                XMPPException.ErrorCondition error)
                                    throws JaxmppException {
                                Log.v(TAG, "MessageCarbons for account " +
                                        sessionObject.getUserBareJid().toString() +
                                        " activation failed = " + error.toString());
                            }

                            @Override
                            public void onSuccess(Stanza responseStanza) throws JaxmppException {
                                Log.v(TAG, "MessageCarbons for account " +
                                        sessionObject.getUserBareJid().toString() +
                                        " activation succeeded");
                            }

                            @Override
                            public void onTimeout() throws JaxmppException {
                                Log.v(TAG, "MessageCarbons for account " +
                                        sessionObject.getUserBareJid().toString() +
                                        " activation timeout");
                            }

                        });
                    } catch (JaxmppException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

    };
    private final Executor taskExecutor = Executors.newFixedThreadPool(1);
    private AccountModifyReceiver accountModifyReceiver = new AccountModifyReceiver();
    private CapabilitiesDBCache capsCache;
    private org.tigase.messenger.jaxmpp.android.chat.ChatProvider chatProvider;
    private ConnectivityManager connManager;
    private DataRemover dataRemover;
    private DatabaseHelper dbHelper;

    private JaxmppCore.LoggedInHandler jaxmppConnectedHandler = new JaxmppCore.LoggedInHandler() {
        @Override
        public void onLoggedIn(SessionObject sessionObject) {
            Log.i("XMPPService", "JAXMPP connected " + sessionObject.getUserBareJid());
            List<Runnable> todo = sessionObject.getProperty(ON_CONNECT_RUNNABLE_ARRAY_KEY);
            try {
                if (todo != null) {
                    for (Runnable runnable : todo) {
                        taskExecutor.execute(runnable);
                    }
                }
            } finally {
                sessionObject.setProperty(ON_CONNECT_RUNNABLE_ARRAY_KEY, null);
            }
        }
    };
    private long keepaliveInterval = 1000 * 60 * 3;
    private HashSet<SessionObject> locked = new HashSet<SessionObject>();
    private AccountManager mAccountManager;
    private final BroadcastReceiver lastAccountActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String account = intent.getStringExtra("account");
            onLastAccountActivityReceived(account);
        }
    };
    private MessageArchiveManagementModule.MessageArchiveItemReceivedEventHandler mamHandler;
    private MessageHandler messageHandler;
    private MessageSender messageSender = new MessageSender(this);
    private MobileModeFeature mobileModeFeature = new MobileModeFeature(this);
    private MucHandler mucHandler;
    private OwnPresenceFactoryImpl ownPresenceStanzaFactory;
    private PresenceHandler presenceHandler;
    private RosterProviderExt rosterProvider;
    private final PresenceModule.SubscribeRequestHandler subscribeHandler = new PresenceModule.SubscribeRequestHandler() {
        @Override
        public void onSubscribeRequest(SessionObject sessionObject, Presence stanza, BareJID jid) {
            XMPPService.this.processSubscriptionRequest(sessionObject, stanza, jid);
        }

    };
    private SSLSocketFactory sslSocketFactory;
    private int usedNetworkType;
    final BroadcastReceiver presenceChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int presenceId = intent.getIntExtra("presence", CPresence.ONLINE);
            if (presenceId == CPresence.OFFLINE) {
                disconnectAllJaxmpp(true);
            } else {
                processPresenceUpdate();
            }
        }
    };
    private final BroadcastReceiver connReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkInfo netInfo = ((ConnectivityManager) context.getSystemService(
                    Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            onNetworkChange(netInfo);
        }

    };
    private JaxmppCore.LoggedOutHandler jaxmppDisconnectedHandler = new JaxmppCore.LoggedOutHandler() {
        @Override
        public void onLoggedOut(SessionObject sessionObject) {
            Jaxmpp jaxmpp = multiJaxmpp.get(sessionObject);
            Log.i("XMPPService", "JAXMPP disconnected " + sessionObject.getUserBareJid());
            if (getUsedNetworkType() != -1) {
                if (jaxmpp != null) {
                    XMPPService.this.connectJaxmpp(jaxmpp, 5 * 1000L);
                }
            }
        }
    };

    public XMPPService() {
        Logger logger = Logger.getLogger("tigase.jaxmpp");
        Handler handler = new AndroidLoggingHandler();
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
        logger.setLevel(Level.ALL);

    }

    private void connectAllJaxmpp(Long delay) {
        setUsedNetworkType(getActiveNetworkType());
        // geolocationFeature.registerLocationListener();

        for (final JaxmppCore jaxmpp : multiJaxmpp.get()) {
            Log.v(TAG, "connecting account " + jaxmpp.getSessionObject().getUserBareJid());
            connectJaxmpp((Jaxmpp) jaxmpp, delay);
        }
    }

    private void connectJaxmpp(final Jaxmpp jaxmpp, final Date date) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        int presenceId = sharedPref.getInt("presence", CPresence.OFFLINE);
        // We are destroying service when going to background but setting status AWAY
        if (presenceId == CPresence.OFFLINE || presenceId == CPresence.AWAY) {
            return;
        }

        if (isLocked(jaxmpp.getSessionObject())) {
            Log.v(TAG,
                    "cancelling connect for " + jaxmpp.getSessionObject().getUserBareJid() +
                            " because it is locked");
            return;
        }

        final Runnable r = new Runnable() {
            @Override
            public void run() {
                Log.v(TAG,
                        "Trying to connect account  " + jaxmpp.getSessionObject().getUserBareJid());

                lock(jaxmpp.getSessionObject(), false);
                if (isDisabled(jaxmpp.getSessionObject())) {
                    Log.v(TAG,
                            "cancelling connect for " + jaxmpp.getSessionObject().getUserBareJid() +
                                    " because it is disabled");
                    return;
                }
                setUsedNetworkType(getActiveNetworkType());
                int tmpNetworkType = getUsedNetworkType();
                Log.v(TAG, "Network state is " + tmpNetworkType);
                if (tmpNetworkType != -1) {
                    taskExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (jaxmpp.isConnected()) {
                                    Log.v(TAG, "cancelling connect for " +
                                            jaxmpp.getSessionObject().getUserBareJid() +
                                            " because it is connected already");
                                    return;
                                }

                                final Connector.State state = jaxmpp.getSessionObject()
                                        .getProperty(Connector.CONNECTOR_STAGE_KEY);
                                Log.v(TAG, "Account " + jaxmpp.getSessionObject().getUserBareJid() +
                                        " is in state " +
                                        state);
                                if (state != null && state != Connector.State.disconnected) {
                                    Log.v(TAG, "cancelling connect for " +
                                            jaxmpp.getSessionObject().getUserBareJid() +
                                            " because it state " + state);
                                    return;
                                }

                                final ArrayList<Runnable> onConnectTasks = new ArrayList<>();

                                onConnectTasks.add(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            if (mobileModeFeature != null) {
                                                mobileModeFeature.accountConnected(jaxmpp);
                                            }
                                        } catch (JaxmppException e) {
                                            Log.e(TAG,
                                                    "Exception processing MobileModeFeature on connect for account " +
                                                            jaxmpp.getSessionObject()
                                                                    .getUserBareJid().toString());
                                        }

                                    }
                                });

                                onConnectTasks
                                        .add(new SendUnsentMessages(XMPPService.this, jaxmpp));
                                onConnectTasks.add(new RejoinToMucRooms(jaxmpp.getSessionObject()));
                                onConnectTasks.add(
                                        new FetchMessageArchiveMAM(XMPPService.this,
                                                jaxmpp.getSessionObject()));

                                jaxmpp.getSessionObject().setProperty("messenger#error", null);
                                setDisconnectionProblemDescription(jaxmpp.getSessionObject(), null);
                                jaxmpp.getSessionObject()
                                        .setProperty(ON_CONNECT_RUNNABLE_ARRAY_KEY, onConnectTasks);
                                jaxmpp.login(false);
                            } catch (Exception e) {
                                if (e.getCause() instanceof SecureTrustManagerFactory.DataCertificateException) {
                                    jaxmpp.getSessionObject()
                                            .setProperty(ACCOUNT_TMP_DISABLED_KEY, Boolean.TRUE);
                                    processCertificateError(jaxmpp,
                                            (SecureTrustManagerFactory.DataCertificateException) e
                                                    .getCause());
                                } else {
                                    Log.e(TAG, "Can't connect account " +
                                                    jaxmpp.getSessionObject().getUserBareJid(),
                                            e);
                                }

                            }

                        }
                    });

                }
            }
        };
        lock(jaxmpp.getSessionObject(), true);

        if (date == null) {
            Log.d(TAG, "Starting connection NOW");
            r.run();
        } else {
            Log.d(TAG, "Starting connection LATER");
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    r.run();
                }
            }, date);
        }
    }

    private void connectJaxmpp(final Jaxmpp jaxmpp, final Long delay) {
        connectJaxmpp(jaxmpp, delay == null ? null : new Date(delay + System.currentTimeMillis()));
    }

    private Jaxmpp createJaxmpp(final BareJID accountJid, final int accountId) {
        final SessionObject sessionObject = new J2SESessionObject();
        sessionObject.setUserProperty(SessionObject.USER_BARE_JID, accountJid);

        try {
            PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = pinfo.versionName;
            sessionObject.setUserProperty(SoftwareVersionModule.VERSION_KEY, versionName);
        } catch (Exception e) {
        }

        PresenceModule.setOwnPresenceStanzaFactory(sessionObject, this.ownPresenceStanzaFactory);
        sessionObject.setUserProperty(Connector.TRUST_MANAGERS_KEY,
                SecureTrustManagerFactory.getTrustManagers(getBaseContext()));

        sessionObject.setUserProperty(SoftwareVersionModule.NAME_KEY,
                getString(R.string.about_application_name));
        sessionObject.setUserProperty(SoftwareVersionModule.OS_KEY,
                "Android " + android.os.Build.VERSION.RELEASE);

        sessionObject.setUserProperty(DiscoveryModule.IDENTITY_CATEGORY_KEY, "client");
        sessionObject.setUserProperty(DiscoveryModule.IDENTITY_TYPE_KEY, "phone");
        sessionObject
                .setUserProperty(CapabilitiesModule.NODE_NAME_KEY, "http://tigase.org/messenger");

        sessionObject.setUserProperty(ACCOUNT_ID, (long) accountId);
        sessionObject.setUserProperty(SocketConnector.SERVER_PORT, 5222);
        sessionObject.setUserProperty(tigase.jaxmpp.j2se.Jaxmpp.CONNECTOR_TYPE, "socket");
        sessionObject.setUserProperty(Connector.EXTERNAL_KEEPALIVE_KEY, true);

        sessionObject.setUserProperty(SocketConnector.SERVER_PORT, 5222);
        sessionObject.setUserProperty(tigase.jaxmpp.j2se.Jaxmpp.CONNECTOR_TYPE, "socket");
        sessionObject.setUserProperty(Connector.EXTERNAL_KEEPALIVE_KEY, true);

        sessionObject.setUserProperty(JaxmppCore.AUTOADD_STANZA_ID_KEY, Boolean.TRUE);

        // sessionObject.setUserProperty(SocketConnector.SSL_SOCKET_FACTORY_KEY,
        // sslSocketFactory);

        final Jaxmpp jaxmpp = new Jaxmpp(sessionObject);
        jaxmpp.getSessionObject()
                .setProperty(SessionObject.Scope.user, SocketConnector.USE_BOUNCYCASTLE_KEY,
                        Boolean.FALSE);
        jaxmpp.setExecutor(executor);

        RosterModule.setRosterStore(sessionObject, new AndroidRosterStore(this.rosterProvider));
        jaxmpp.getModulesManager().register(new RosterModule(this.rosterProvider));
        PresenceModule.setPresenceStore(sessionObject, new J2SEPresenceStore());
        jaxmpp.getModulesManager().register(new PresenceModule());
        jaxmpp.getModulesManager().register(new VCardModule());
        jaxmpp.getModulesManager().register(new AdHocCommansModule());
        jaxmpp.getModulesManager().register(new PushNotificationModule());
        jaxmpp.getModulesManager().register(new MessageArchivingModule());
        jaxmpp.getModulesManager().register(new MessageArchiveManagementModule());
        jaxmpp.getModulesManager().register(new HttpFileUploadModule());

        AndroidChatManager chatManager = new AndroidChatManager(this.chatProvider);
        MessageModule messageModule = new MessageModule(chatManager);
        jaxmpp.getModulesManager().register(messageModule);

        messageModule.addExtension(new ChatStateExtension(chatManager));

        jaxmpp.getModulesManager()
                .register(new MucModule(new AndroidRoomsManager(this.chatProvider)));
        jaxmpp.getModulesManager().register(new PingModule());
        jaxmpp.getModulesManager().register(new EntityTimeModule());

        CapabilitiesModule capsModule = new CapabilitiesModule();
        capsModule.setCache(capsCache);
        jaxmpp.getModulesManager().register(capsModule);

        try {
            jaxmpp.getModulesManager().register(new MessageCarbonsModule());
        } catch (JaxmppException ex) {
            Log.v(TAG, "Exception creating instance of MessageCarbonsModule", ex);
        }

        return jaxmpp;
    }

    private void disconnectAllJaxmpp(final boolean cleaning) {
        setUsedNetworkType(-1);
        // if (geolocationFeature != null) {
        // geolocationFeature.unregisterLocationListener();
        // }
        for (final JaxmppCore j : multiJaxmpp.get()) {
            disconnectJaxmpp((Jaxmpp) j, cleaning);
        }

        // synchronized (connectionErrorsCounter) {
        // connectionErrorsCounter.clear();
        // }
    }

    private void disconnectJaxmpp(final Jaxmpp jaxmpp, final boolean cleaning) {
        taskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // geolocationFeature.accountDisconnect(jaxmpp);
                    if (jaxmpp.isConnected()) {
                        jaxmpp.disconnect(false);
                    }
                    // is this needed any more??
                    if (cleaning || !StreamManagementModule
                            .isResumptionEnabled(jaxmpp.getSessionObject())) {
                        XMPPService.this.rosterProvider.resetStatus(jaxmpp.getSessionObject());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "cant; disconnect account " +
                            jaxmpp.getSessionObject().getUserBareJid(), e);
                }

            }
        });
    }

    private int getActiveNetworkType() {
        NetworkInfo info = connManager.getActiveNetworkInfo();
        if (info == null) {
            return -1;
        }
        if (!info.isConnected()) {
            return -1;
        }
        return info.getType();
    }

    DisconnectionCauses getDisconectionProblemDescription(Account accout) {
        String tmp = mAccountManager.getUserData(accout, AccountsConstants.DISCONNECTION_CAUSE_KEY);
        if (tmp == null) {
            return null;
        } else {
            return DisconnectionCauses.valueOf(tmp);
        }
    }

    public Jaxmpp getJaxmpp(String account) {
        return this.multiJaxmpp.get(BareJID.bareJIDInstance(account));
    }

    public Jaxmpp getJaxmpp(BareJID account) {
        return this.multiJaxmpp.get(account);
    }

    public MultiJaxmpp getMultiJaxmpp() {
        return this.multiJaxmpp;
    }

    protected final Connector.State getState(SessionObject object) {
        Connector.State state = multiJaxmpp.get(object).getSessionObject()
                .getProperty(Connector.CONNECTOR_STAGE_KEY);
        return state == null ? Connector.State.disconnected : state;
    }

    private int getUsedNetworkType() {
        return this.usedNetworkType;
    }

    private void setUsedNetworkType(int type) {
        this.usedNetworkType = type;
    }

    public boolean isDisabled(SessionObject sessionObject) {
        Boolean x = sessionObject.getProperty(ACCOUNT_TMP_DISABLED_KEY);
        return x == null ? false : x;
    }

    private boolean isLocked(SessionObject sessionObject) {
        synchronized (locked) {
            return locked.contains(sessionObject);
        }
    }

    private void keepAlive() {
        taskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (JaxmppCore jaxmpp : multiJaxmpp.get()) {
                    try {
                        if (jaxmpp.isConnected()) {
                            Log.i("XMPPService", "Sending keepAlive for " +
                                    jaxmpp.getSessionObject().getUserBareJid());
                            jaxmpp.getConnector().keepalive();
                            // GeolocationFeature.sendQueuedGeolocation(jaxmpp,
                            // JaxmppService.this);
                        } else if (Connector.State.disconnecting ==
                                jaxmpp.getSessionObject()
                                        .getProperty(Connector.CONNECTOR_STAGE_KEY)) {
                            // if jaxmpp hangs on 'disconnecting' state for more
                            // than 45 seconds, stop Connector.
                            final Date x = jaxmpp.getSessionObject()
                                    .getProperty(Connector.CONNECTOR_STAGE_TIMESTAMP_KEY);
                            if (x != null && x.getTime() < System.currentTimeMillis() - 45 * 1000) {
                                jaxmpp.getConnector().stop(true);
                            }
                        }
                    } catch (JaxmppException ex) {
                        Log.e(TAG,
                                "error sending keep alive for = " +
                                        jaxmpp.getSessionObject().getUserBareJid().toString(),
                                ex);
                    }
                }
            }
        });
    }

    private void lock(SessionObject sessionObject, boolean value) {
        synchronized (locked) {
            if (value) {
                locked.add(sessionObject);
            } else {
                locked.remove(sessionObject);
            }
        }
    }

    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mAccountManager = AccountManager.get(this);

        Log.i("XMPPService", "Service started");

        this.ownPresenceStanzaFactory = new OwnPresenceFactoryImpl();
        this.dbHelper = DatabaseHelper.getInstance(this);
        this.connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        this.dataRemover = new DataRemover(this.dbHelper);

        SSLSessionCache sslSessionCache = new SSLSessionCache(this);
        this.sslSocketFactory = SSLCertificateSocketFactory.getDefault(0, sslSessionCache);

        this.rosterProvider = new RosterProviderExt(this, dbHelper,
                new RosterProviderExt.Listener() {
                    @Override
                    public void onChange(Long rosterItemId) {
                        Uri uri = rosterItemId != null
                                ? ContentUris.withAppendedId(TigaseSdk.getInstance().getRosterUri(),
                                rosterItemId) : TigaseSdk.getInstance().getRosterUri();

                        Log.i(TAG, "Content change: " + uri);
                        TigaseSdk.getInstance().getApplicationContext().getContentResolver()
                                .notifyChange(uri, null);

                    }
                }, "roster_version");
        rosterProvider.resetStatus();

        this.presenceHandler = new PresenceHandler(this);
        this.messageHandler = new MessageHandler(this);
        this.mamHandler = new MAMHandler(this);
        this.chatProvider = new org.tigase.messenger.jaxmpp.android.chat.ChatProvider(this,
                dbHelper,
                new org.tigase.messenger.jaxmpp.android.chat.ChatProvider.Listener() {
                    @Override
                    public void onChange(Long chatId) {
                        Uri uri = chatId != null ? ContentUris.withAppendedId(
                                TigaseSdk.getInstance().getOpenChatsUri(), chatId)
                                : TigaseSdk.getInstance().getOpenChatsUri();
                        TigaseSdk.getInstance().getApplicationContext().getContentResolver()
                                .notifyChange(uri, null);
                    }
                });
        chatProvider.resetRoomState(CPresence.OFFLINE);
        this.mucHandler = new MucHandler();
        this.capsCache = new CapabilitiesDBCache(dbHelper);

        IntentFilter screenStateReceiverFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenStateReceiverFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenStateReceiver, screenStateReceiverFilter);

        registerReceiver(lastAccountActivityReceiver, new IntentFilter(LAST_ACCOUNT_ACTIVITY));
        registerReceiver(presenceChangedReceiver, new IntentFilter(CLIENT_PRESENCE_CHANGED_ACTION));
        registerReceiver(connReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

        multiJaxmpp.addHandler(MessageModule.ChatUpdatedHandler.ChatUpdatedEvent.class,
                (so, chat) -> chatProvider.updateChat(chat));
        multiJaxmpp.addHandler(
                StreamManagementModule.StreamManagementFailedHandler.StreamManagementFailedEvent.class,
                new StreamManagementModule.StreamManagementFailedHandler() {
                    @Override
                    public void onStreamManagementFailed(final SessionObject sessionObject,
                                                         XMPPException.ErrorCondition condition) {
                        if (condition != null &&
                                condition.getElementName().equals("item-not-found")) {
                            XMPPService.this.rosterProvider.resetStatus(sessionObject);
                        }
                    }
                });
        multiJaxmpp.addHandler(
                DiscoveryModule.ServerFeaturesReceivedHandler.ServerFeaturesReceivedEvent.class,
                streamHandler);
        multiJaxmpp
                .addHandler(JaxmppCore.LoggedInHandler.LoggedInEvent.class, jaxmppConnectedHandler);
        multiJaxmpp.addHandler(JaxmppCore.LoggedOutHandler.LoggedOutEvent.class,
                jaxmppDisconnectedHandler);
        // multiJaxmpp.addHandler(SocketConnector.ErrorHandler.ErrorEvent.class,
        // );
        //
        // this.connectorListener = new Connector.ErrorHandler() {
        //
        // @Override
        // public void onError(SessionObject sessionObject, StreamError
        // condition, Throwable caught) throws JaxmppException {
        // AbstractSocketXmppSessionLogic.this.processConnectorErrors(condition,
        // caught);
        // }
        // };
        multiJaxmpp.addHandler(PresenceModule.ContactAvailableHandler.ContactAvailableEvent.class,
                presenceHandler);
        multiJaxmpp
                .addHandler(PresenceModule.ContactUnavailableHandler.ContactUnavailableEvent.class,
                        presenceHandler);
        multiJaxmpp.addHandler(
                PresenceModule.ContactChangedPresenceHandler.ContactChangedPresenceEvent.class,
                presenceHandler);
        // All contacts are subscriptions by default, no user managed adding
//		multiJaxmpp.addHandler(PresenceModule.SubscribeRequestHandler.SubscribeRequestEvent.class, subscribeHandler);

        multiJaxmpp.addHandler(MessageModule.MessageReceivedHandler.MessageReceivedEvent.class,
                messageHandler);
        multiJaxmpp.addHandler(
                MessageArchiveManagementModule.MessageArchiveItemReceivedEventHandler.MessageArchiveItemReceivedEvent.class,
                mamHandler);
        multiJaxmpp.addHandler(MessageCarbonsModule.CarbonReceivedHandler.CarbonReceivedEvent.class,
                messageHandler);
        multiJaxmpp
                .addHandler(ChatStateExtension.ChatStateChangedHandler.ChatStateChangedEvent.class,
                        messageHandler);
        multiJaxmpp.addHandler(AuthModule.AuthFailedHandler.AuthFailedEvent.class,
                new AuthModule.AuthFailedHandler() {

                    @Override
                    public void onAuthFailed(SessionObject sessionObject,
                                             SaslModule.SaslError error) throws JaxmppException {
                        processAuthenticationError((Jaxmpp) multiJaxmpp.get(sessionObject));
                    }
                });

        multiJaxmpp.addHandler(MucModule.MucMessageReceivedHandler.MucMessageReceivedEvent.class,
                mucHandler);
        multiJaxmpp
                .addHandler(MucModule.OccupantLeavedHandler.OccupantLeavedEvent.class, mucHandler);
        multiJaxmpp.addHandler(MucModule.MessageErrorHandler.MessageErrorEvent.class, mucHandler);
        multiJaxmpp.addHandler(MucModule.YouJoinedHandler.YouJoinedEvent.class, mucHandler);
        multiJaxmpp.addHandler(MucModule.StateChangeHandler.StateChangeEvent.class, mucHandler);
        multiJaxmpp.addHandler(MucModule.PresenceErrorHandler.PresenceErrorEvent.class, mucHandler);
        multiJaxmpp
                .addHandler(MucModule.NewRoomCreatedHandler.NewRoomCreatedEvent.class, mucHandler);
        multiJaxmpp.addHandler(MucModule.MucMessageReceivedHandler.MucMessageReceivedEvent.class,
                mucHandler);
        multiJaxmpp.addHandler(MucModule.InvitationReceivedHandler.InvitationReceivedEvent.class,
                mucHandler);
        //multiJaxmpp.addHandler(MucModule.StateChangeHandler.StateChangeEvent.class, mucHandler);

        IntentFilter filterAccountModifyReceiver = new IntentFilter();
        filterAccountModifyReceiver.addAction(AccountsConstants.ACCOUNT_MODIFIED_MSG);
        filterAccountModifyReceiver.addAction(AccountManager.LOGIN_ACCOUNTS_CHANGED_ACTION);
        registerReceiver(accountModifyReceiver, filterAccountModifyReceiver);

        startKeepAlive();

        updateJaxmppInstances();
        // connectAllJaxmpp();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        Log.i("XMPPService", "Service destroyed");
        EventBus.getDefault().unregister(this);

        stopKeepAlive();

        unregisterReceiver(lastAccountActivityReceiver);
        unregisterReceiver(screenStateReceiver);
        unregisterReceiver(connReceiver);
        unregisterReceiver(presenceChangedReceiver);
        unregisterReceiver(accountModifyReceiver);

        disconnectAllJaxmpp(true);

        super.onDestroy();
        mobileModeFeature = null;
    }

    private void onLastAccountActivityReceived(final String accountName) {
        final long t = System.currentTimeMillis();

        Jaxmpp jaxmpp = getJaxmpp(accountName);
        if (jaxmpp == null || !jaxmpp.isConnected()) {
            return;
        }

        Account account = AccountHelper.getAccount(mAccountManager, accountName);
        mAccountManager.setUserData(account, AccountsConstants.FIELD_LAST_ACTIVITY, "" + t);
    }

    private void onNetworkChange(final NetworkInfo netInfo) {
        if (netInfo != null && netInfo.isConnected()) {
            connectAllJaxmpp(5000l);
        } else {
            disconnectAllJaxmpp(false);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && (MessageSender.SEND_CHAT_MESSAGE_ACTION.equals(intent.getAction()) ||
                MessageSender.SEND_GROUPCHAT_MESSAGE_ACTION.equals(intent.getAction()))) {
            messageSender.process(this, intent);
        } /*else if (intent != null && SEND_FILE_ACTION.equals(intent.getAction())) {
			String account = intent.getStringExtra("account");
			Uri content = intent.getParcelableExtra("content");
			String roomJid = intent.getStringExtra("roomJID");
			int chatId = intent.getIntExtra("chatId", Integer.MIN_VALUE);
			if (roomJid != null) {
				uploadFileAndSend(account, content, BareJID.bareJIDInstance(roomJid));
			} else if (chatId != Integer.MIN_VALUE) {
				uploadFileAndSend(account, content, chatId);
			}
        }*/ else if (intent != null && CONNECT_ALL.equals(intent.getAction())) {
            final boolean destroyed = intent.getBooleanExtra("destroyed", false);
            for (Account account : mAccountManager
                    .getAccountsByType(TigaseSdk.getInstance().getPackageName())) {
                String tmp = mAccountManager
                        .getUserData(account, AccountsConstants.PUSH_NOTIFICATION);
                boolean pushEnabled = tmp == null ? false : Boolean.parseBoolean(tmp);
                if (destroyed && !pushEnabled || !destroyed) {
                    Jaxmpp jaxmpp = getJaxmpp(account.name);
                    connectJaxmpp(jaxmpp, (Date) null);
                }
            }
        } else if (intent != null && CONNECT_SINGLE.equals(intent.getAction())) {
            String ac = intent.getStringExtra("account");
            Jaxmpp jaxmpp = getJaxmpp(ac);
            connectJaxmpp(jaxmpp, (Date) null);
        } else if (intent != null && KEEPALIVE_ACTION.equals(intent.getAction())) {
            keepAlive();
        }

//		startForeground(9876, NotificationHandler.getInstance().getForegroundServiceNotification(this));
        return super.onStartCommand(intent, flags, startId);
    }

    private void processAuthenticationError(final Jaxmpp jaxmpp) {
        HyperLog.getInstance().e(TAG, "processAuthenticationError",
                "Invalid credentials of account " + jaxmpp.getSessionObject().getUserBareJid());
        jaxmpp.getSessionObject().setUserProperty(ACCOUNT_TMP_DISABLED_KEY, true);

        setDisconnectionProblemDescription(jaxmpp.getSessionObject(),
                DisconnectionCauses.AUTHENTICATION);

        String title = getString(R.string.notification_credentials_error_title,
                jaxmpp.getSessionObject().getUserBareJid().toString());
        String text = getString(R.string.notification_certificate_error_text);
        NotificationHandler.getInstance().notifyError(title, text);
    }

    private void processCertificateError(final Jaxmpp jaxmpp,
                                         final SecureTrustManagerFactory.DataCertificateException cause) {
        HyperLog.getInstance().e(TAG, "processCertificateError",
                "Invalid certificate of account " + jaxmpp.getSessionObject().getUserBareJid() +
                        ": " + cause.getMessage());

        setDisconnectionProblemDescription(jaxmpp.getSessionObject(),
                DisconnectionCauses.CERTIFICATE_ERROR);

        jaxmpp.getSessionObject().setUserProperty(ACCOUNT_TMP_DISABLED_KEY, true);

        String title = getString(R.string.notification_certificate_error_title,
                jaxmpp.getSessionObject().getUserBareJid().toString());
        String text = getString(R.string.notification_certificate_error_text);
        NotificationHandler.getInstance().notifyError(title, text);
    }

    void processPresenceUpdate() {
        taskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (JaxmppCore jaxmpp : multiJaxmpp.get()) {
                    try {
                        if (!jaxmpp.isConnected()) {
                            connectJaxmpp((Jaxmpp) jaxmpp, (Long) null);
                        } else {
                            jaxmpp.getModule(PresenceModule.class).sendInitialPresence();
                        }
                    } catch (JaxmppException e) {
                        Log.e("TAG", "Can't update presence", e);
                    }
                }
            }
        });
    }

    private void processSubscriptionRequest(final SessionObject sessionObject,
                                            final Presence stanza,
                                            final BareJID jid) {
        Log.e(TAG, "Subscription request from  " + jid);
        retrieveVCard(sessionObject, jid);
        // TODO: auto accept new subscription
        // TODO: maybe: send notification that new contact is added
    }

    private void retrieveVCard(final SessionObject sessionObject, final BareJID jid) {
        try {
            JaxmppCore jaxmpp = multiJaxmpp.get(sessionObject);
            if (jaxmpp == null || !jaxmpp.isConnected()) {
                return;
            }
            // final RosterItem rosterItem = jaxmpp.getRoster().get(jid);
            VCardModule vcardModule = jaxmpp.getModule(VCardModule.class);
            if (vcardModule != null) {
                vcardModule.retrieveVCard(JID.jidInstance(jid), (long) 3 * 60 * 1000,
                        new VCardModule.VCardAsyncCallback() {

                            @Override
                            public void onError(Stanza responseStanza,
                                                XMPPException.ErrorCondition error)
                                    throws JaxmppException {
                            }

                            @Override
                            public void onTimeout() throws JaxmppException {
                            }

                            @Override
                            protected void onVCardReceived(VCard vcard) throws XMLException {
                                try {
                                    if (vcard.getPhotoVal() != null &&
                                            vcard.getPhotoVal().length() > 0) {
                                        byte[] buffer = tigase.jaxmpp.core.client.Base64.decode(
                                                vcard.getPhotoVal());

                                        updateVCardHash(sessionObject, jid, buffer);
                                    }
                                } catch (Exception e) {
                                    Log.e("tigase", "WTF?", e);
                                }
                            }
                        });
            }
        } catch (Exception e) {
            Log.e("tigase", "WTF?", e);
        }
    }

    private void sendAcks() {
        taskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (JaxmppCore jaxmpp : multiJaxmpp.get()) {
                    try {
                        if (jaxmpp.isConnected()) {
                            Log.d("XMPPService", "Sending ACK for " +
                                    jaxmpp.getSessionObject().getUserBareJid());
                            jaxmpp.getModule(StreamManagementModule.class).sendAck();
                        }
                    } catch (JaxmppException ex) {
                        Log.e(TAG, "error sending ACK for = " +
                                        jaxmpp.getSessionObject().getUserBareJid().toString(),
                                ex);
                    }
                }
            }
        });
    }

    void setDisconnectionProblemDescription(SessionObject sessionObject,
                                            DisconnectionCauses cause) {
        setDisconnectionProblemDescription(
                AccountHelper
                        .getAccount(mAccountManager, sessionObject.getUserBareJid().toString()),
                cause);
    }

    void setDisconnectionProblemDescription(Account accout, DisconnectionCauses cause) {
        mAccountManager.setUserData(accout, AccountsConstants.DISCONNECTION_CAUSE_KEY,
                cause == null ? null : cause.name());
    }

    private void startKeepAlive() {
        Intent i = new Intent();
        i.setClass(this, XMPPService.class);
        i.setAction(KEEPALIVE_ACTION);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);

        AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + keepaliveInterval,
                keepaliveInterval, pi);
    }

    private void stopKeepAlive() {
        Intent i = new Intent();
        i.setClass(this, XMPPService.class);
        i.setAction(KEEPALIVE_ACTION);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmMgr.cancel(pi);
    }

    private boolean storeMessage(SessionObject sessionObject, Chat chat,
                                 tigase.jaxmpp.core.client.xmpp.stanzas.Message msg) throws XMLException {
        XmppDelay delay = XmppDelay.extract(msg);
        Date t = ((delay == null || delay.getStamp() == null) ? new Date() : delay.getStamp());
        return storeMessage(sessionObject, chat, msg, t);
    }

    private boolean storeMessage(SessionObject sessionObject, Chat chat,
                                 tigase.jaxmpp.core.client.xmpp.stanzas.Message msg, Date timeStamp)
            throws XMLException {
        // for now let's ignore messages without body element
        if (msg.getBody() == null && msg.getType() != StanzaType.error) {
            return false;
        }
        BareJID authorJid =
                msg.getFrom() == null ? sessionObject.getUserBareJid() : msg.getFrom().getBareJid();
        String author = authorJid.toString();
        String jid = null;
        if (chat != null) {
            jid = chat.getJid().getBareJid().toString();
        } else {
            jid = (sessionObject.getUserBareJid().equals(authorJid) ? msg.getTo()
                    .getBareJid() : authorJid).toString();
        }

        ContentValues values = new ContentValues();
        values.put(DatabaseContract.ChatHistory.FIELD_AUTHOR_JID, author);
        values.put(DatabaseContract.ChatHistory.FIELD_JID, jid);

        values.put(DatabaseContract.ChatHistory.FIELD_TIMESTAMP, timeStamp.getTime());

        if (msg.getType() == StanzaType.error) {

            if (chat == null) {
                Log.e(TAG, "Error message from " + jid + " has no Chat. Skipping store.");
                return false;
            }

            ErrorElement error = ErrorElement.extract(msg);
            String body = "Error: ";
            if (error != null) {
                if (error.getText() != null) {
                    body += error.getText();
                } else {
                    XMPPException.ErrorCondition errorCondition = error.getCondition();
                    if (errorCondition != null) {
                        body += errorCondition.getElementName();
                    }
                }
            }
            if (msg.getBody() != null) {
                body += "\n ------ \n";
                body += Crypto.decodeBody(msg.getBody());
            }
            values.put(DatabaseContract.ChatHistory.FIELD_BODY, body);
            values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
                    DatabaseContract.ChatHistory.ITEM_TYPE_ERROR);
        } else {
            values.put(DatabaseContract.ChatHistory.FIELD_BODY, Crypto.decodeBody(msg.getBody()));

            Element geoloc = msg.getChildrenNS("geoloc", "http://jabber.org/protocol/geoloc");
            Element oobData = msg.getChildrenNS("x", "jabber:x:oob");
            if (oobData != null) {
                Element url = oobData.getFirstChild("url");
                if (url != null) {
                    values.put(DatabaseContract.ChatHistory.FIELD_DATA, url.getAsString());
                    String mimeType = FileUploaderTask.guessMimeType(url.getValue());
                    if (mimeType != null && mimeType.startsWith("image/")) {
                        values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
                                DatabaseContract.ChatHistory.ITEM_TYPE_IMAGE);
                    } else if (mimeType != null && mimeType.startsWith("video/")) {
                        values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
                                DatabaseContract.ChatHistory.ITEM_TYPE_VIDEO);
                    } else {
                        values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
                                DatabaseContract.ChatHistory.ITEM_TYPE_FILE);
                    }
                }
            } else if (geoloc != null) {
                values.put(DatabaseContract.ChatHistory.FIELD_DATA, geoloc.getAsString());
                values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
                        DatabaseContract.ChatHistory.ITEM_TYPE_LOCALITY);
            } else {
                values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
                        DatabaseContract.ChatHistory.ITEM_TYPE_MESSAGE);
            }
        }

        values.put(DatabaseContract.ChatHistory.FIELD_STANZA_ID, msg.getId());
        if (chat != null) {
            values.put(DatabaseContract.ChatHistory.FIELD_THREAD_ID, chat.getThreadId());
        }
        values.put(DatabaseContract.ChatHistory.FIELD_ACCOUNT,
                sessionObject.getUserBareJid().toString());

        // DatabaseContract.ChatHistory.ITEM_TYPE_MESSAGE;

        if (sessionObject.getUserBareJid().equals(authorJid)) {
            values.put(DatabaseContract.ChatHistory.FIELD_STATE,
                    DatabaseContract.ChatHistory.STATE_OUT_SENT);
        } else {
            values.put(DatabaseContract.ChatHistory.FIELD_STATE,
                    NotificationHandler.getInstance().isFocusedOnChatId(chat.getId())
                            ? DatabaseContract.ChatHistory.STATE_INCOMING
                            : DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD);
        }

        Uri uri = Uri
                .parse(TigaseSdk.getInstance().getChatHistoryUri() + "/" +
                        sessionObject.getUserBareJid() +
                        "/" +
                        jid);
        uri = getContentResolver().insert(uri, values);

        TigaseSdk.getInstance().getApplicationContext().getContentResolver()
                .notifyChange(ContentUris.withAppendedId(TigaseSdk.getInstance().getOpenChatsUri(),
                        chat.getId()), null);

        EventBus.getDefault().post(new DownloadFileEvent(Crypto.decodeBody(msg.getBody()),
                sessionObject.getUserBareJid().getLocalpart(), new DownloadedFileEvent(uri)));

        return true;
    }

    private void storeMucSysMsg(SessionObject sessionObject, Room room, String body) {
        try {
            if (body == null || body == null || room == null) {
                return;
            }

            ContentValues values = new ContentValues();
            values.put(DatabaseContract.ChatHistory.FIELD_JID, room.getRoomJid().toString());
            values.put(DatabaseContract.ChatHistory.FIELD_TIMESTAMP, (new Date()).getTime());

            values.put(DatabaseContract.ChatHistory.FIELD_STATE,
                    NotificationHandler.getInstance().isFocusedOnRoomId(room.getId())
                            ? DatabaseContract.ChatHistory.STATE_INCOMING
                            : DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD);
            values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
                    DatabaseContract.ChatHistory.ITEM_TYPE_ERROR);
            values.put(DatabaseContract.ChatHistory.FIELD_CHAT_TYPE,
                    DatabaseContract.ChatHistory.CHAT_TYPE_MUC);

            values.put(DatabaseContract.ChatHistory.FIELD_BODY, body);

            values.put(DatabaseContract.ChatHistory.FIELD_ACCOUNT,
                    sessionObject.getUserBareJid().toString());

            Uri uri = Uri
                    .parse(TigaseSdk.getInstance().getMucHistoryUri() + "/" +
                            sessionObject.getUserBareJid() +
                            "/" +
                            Uri.encode(room.getRoomJid().toString()));
            Uri x = getContentResolver().insert(uri, values);
        } catch (Exception ex) {
            Log.e(TAG, "Exception handling received MUC message", ex);
        }

    }


    private final void updateJaxmppInstances() {
        final HashSet<BareJID> accountsJids = new HashSet<BareJID>();
        for (JaxmppCore jaxmpp : multiJaxmpp.get()) {
            accountsJids.add(jaxmpp.getSessionObject().getUserBareJid());
        }

        for (Account account : mAccountManager
                .getAccountsByType(TigaseSdk.getInstance().getPackageName())) {
            BareJID accountJid = BareJID.bareJIDInstance(account.name);
            Jaxmpp jaxmpp = multiJaxmpp.get(accountJid);
            if (jaxmpp == null) {
                jaxmpp = createJaxmpp(accountJid, account.hashCode());
                multiJaxmpp.add(jaxmpp);
            }

            // workaround for unknown certificate error
            jaxmpp.getSessionObject().setProperty("jaxmpp#ThrowedException", null);

            String tmp = mAccountManager
                    .getUserData(account, AccountsConstants.AUTOMATIC_PRIORITIES);
            final boolean automaticPriorities = tmp == null ? true : Boolean.parseBoolean(tmp);
            if (automaticPriorities) {
                jaxmpp.getSessionObject()
                        .setUserProperty(CUSTOM_PRIORITIES_ENTITY_KEY, new PrioritiesEntity());
            } else {
                PrioritiesEntity pr = PrioritiesEntity.instance(
                        mAccountManager.getUserData(account, AccountsConstants.CUSTOM_PRIORITIES));
                jaxmpp.getSessionObject().setUserProperty(CUSTOM_PRIORITIES_ENTITY_KEY, pr);
            }

            String password = mAccountManager.getPassword(account);
            String nickname = mAccountManager
                    .getUserData(account, AccountsConstants.FIELD_NICKNAME);
            String hostname = mAccountManager
                    .getUserData(account, AccountsConstants.FIELD_HOSTNAME);
            String resource = mAccountManager
                    .getUserData(account, AccountsConstants.FIELD_RESOURCE);

            jaxmpp.getSessionObject().setUserProperty(SessionObject.PASSWORD, password);
            jaxmpp.getSessionObject().setUserProperty(SessionObject.NICKNAME, nickname);
            if (TextUtils.isEmpty(hostname)) {
                hostname = null;
            }
            jaxmpp.getSessionObject().setUserProperty(SocketConnector.SERVER_HOST, hostname);

            if (TextUtils.isEmpty(resource)) {
                resource = null;
            }
            jaxmpp.getSessionObject().setUserProperty(SessionObject.RESOURCE, resource);

            MobileModeFeature.updateSettings(account, jaxmpp, this);

            boolean disabled = !Boolean.parseBoolean(
                    mAccountManager.getUserData(account, AccountsConstants.FIELD_ACTIVE));
            jaxmpp.getSessionObject().setUserProperty(ACCOUNT_TMP_DISABLED_KEY, disabled);

            if (disabled) {
                if (jaxmpp.isConnected()) {
                    this.disconnectJaxmpp(jaxmpp, true);
                }
            } else {
                if (!jaxmpp.isConnected()) {
                    this.connectJaxmpp(jaxmpp, 1L);
                }
            }

            accountsJids.remove(accountJid);
        }

        for (BareJID accountJid : accountsJids) {
            final Jaxmpp jaxmpp = multiJaxmpp.get(accountJid);
            if (jaxmpp != null) {
                multiJaxmpp.remove(jaxmpp);
                taskExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            jaxmpp.disconnect();
                            // clear presences for account?
                            // app.clearPresences(jaxmpp.getSessionObject(),
                            // false);
                            // is this needed any more??
                            // JaxmppService.this.rosterProvider.resetStatus(jaxmpp.getSessionObject());
                        } catch (Exception ex) {
                            Log.e(TAG, "Can't disconnect", ex);
                        }

                    }
                });
            }
        }

        dataRemover.removeUnusedData(this);
    }

    protected synchronized void updateRosterItem(final SessionObject sessionObject,
                                                 final Presence p)
            throws XMLException {
        if (p != null) {
            Element x = p.getChildrenNS("x", "vcard-temp:x:update");
            if (x != null) {
                for (Element c : x.getChildren()) {
                    if (c.getName().equals("photo") && c.getValue() != null) {
                        boolean retrieve = false;
                        final String sha = c.getValue();
                        if (sha == null) {
                            continue;
                        }
                        retrieve = !rosterProvider
                                .checkVCardHash(sessionObject, p.getFrom().getBareJid(), sha);

                        if (retrieve) {
                            retrieveVCard(sessionObject, p.getFrom().getBareJid());
                        }
                    }
                }
            }
        }

        // Synchronize contact status
        BareJID from = p.getFrom().getBareJid();
        PresenceStore store = PresenceModule.getPresenceStore(sessionObject);
        Presence bestPresence = store.getBestPresence(from);
        // SyncAdapter.syncContactStatus(getApplicationContext(),
        // sessionObject.getUserBareJid(), from, bestPresence);
    }

    public void updateVCardHash(SessionObject sessionObject, BareJID jid, byte[] buffer) {
        rosterProvider.updateVCardHash(sessionObject, jid, buffer);
        Intent intent = new Intent("org.tigase.messenger.phone.pro.AvatarUpdated");
        intent.putExtra("jid", jid.toString());
        XMPPService.this.sendBroadcast(intent);
    }

    @Subscribe
    public void onMessageEvent(DownloadedFileEvent event) {
        Uri chatHistoryUri = event.getChatHistoryUri();
        final ContentValues values = new ContentValues();
        values.put(DatabaseContract.ChatHistory.FIELD_INTERNAL_CONTENT_URI, event.getMediaUri());
        getContentResolver().update(chatHistoryUri, values, null, null);
        getContentResolver().notifyChange(chatHistoryUri, null);
    }

    @Subscribe
    public void onMessageEvent(UploadedFileEvent event) {
        Message msg = event.getMsg();
        ContentValues values = event.getValues();
        String fileId = event.getFileId();
        int chatId = event.getChatId();

        if (event.isGroupIM()) {
            try {
                String displayName = fileId + ".jpg";
                //TODO: check: fileId or displayName?
                SendUnsentMessages
                        .addOOB(DatabaseContract.ChatHistory.ITEM_TYPE_IMAGE,
                                "<url>" + fileId + "</url>", msg, values);
                String bd = fileId + (msg.getBody() == null ? "" : "\n" + msg.getBody());
                values.put(DatabaseContract.ChatHistory.FIELD_BODY, bd);
                msg.setBody(Crypto.encodeBody(bd));
//                send(id, msg, values);
                Room room = event.getRoom();
                room.sendMessage(msg);
                values.put(DatabaseContract.ChatHistory.FIELD_STATE,
                        DatabaseContract.ChatHistory.STATE_OUT_SENT);
                getContentResolver()
                        .update(Uri.parse(TigaseSdk.getInstance().getMucHistoryUri() + "/" +
                                room.getSessionObject().getUserBareJid() + "/" +
                                msg.getTo().getBareJid() + "/" + chatId), values, null, null);
            } catch (Exception e) {
                HyperLog.getInstance().e(TAG, "onMessageEvent: UploadedFileEvent", e);
            }
        } else {
            try {
                String displayName = fileId + ".jpg";
                SendUnsentMessages.addOOB(DatabaseContract.ChatHistory.ITEM_TYPE_IMAGE,
                        "<url>" + displayName + "</url>", msg, values);
                String bd = fileId;
                values.put(DatabaseContract.ChatHistory.FIELD_BODY, bd);
                msg.setBody(Crypto.encodeBody(bd));

                //send(id, msg, values);
                Jaxmpp jaxmpp = event.getJaxmpp();
                final MessageModule messageModule = jaxmpp.getModule(MessageModule.class);
                messageModule.sendMessage(msg);
                values.put(DatabaseContract.ChatHistory.FIELD_STATE,
                        DatabaseContract.ChatHistory.STATE_OUT_SENT);
                getContentResolver()
                        .update(Uri.parse(TigaseSdk.getInstance().getChatHistoryUri() + "/" +
                                jaxmpp.getSessionObject().getUserBareJid() + "/" +
                                msg.getTo().getBareJid() + "/" + chatId), values, null, null);
            } catch (JaxmppException e) {
                HyperLog.getInstance().e(TAG, "onMessageEvent: UploadedFileEvent", e);
            }
        }
    }

    private class AccountModifyReceiver
            extends BroadcastReceiver
            implements Runnable {

        private String account;
        private boolean forceDisconnect;

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("XMPPService", "Updating accounts !" + intent.getAction());

            // TODO: test this scenario, seems like it was error
            this.account = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            this.forceDisconnect = intent
                    .getBooleanExtra(AccountsConstants.KEY_FORCE_DISCONNECT, false);

            (new Thread(this)).start();
        }

        @Override
        public void run() {
            Log.i("XMPPService", "Updating account");
            if (account != null && forceDisconnect) {
                try {
                    multiJaxmpp.get(BareJID.bareJIDInstance(account)).disconnect();
                } catch (java.lang.NullPointerException | JaxmppException e) {
                    Log.i("XMPPService", "Problem during disconnecting!", e);
                }
            }

            updateJaxmppInstances();
            for (JaxmppCore j : multiJaxmpp.get()) {
                Connector.State st = getState(j.getSessionObject());
                if (st == Connector.State.disconnected || st == null) {
                    connectJaxmpp((Jaxmpp) j, (Long) null);
                } else {
                    try {
                        j.getModule(PresenceModule.class).sendInitialPresence();
                    } catch (JaxmppException e) {
                        Log.e("XMPPService", "Cannot resend initial presence", e);
                    }
                }
            }
        }

    }

    public class LocalBinder
            extends Binder {

        public XMPPService getService() {
            // Return this instance of LocalService so clients can call public
            // methods
            return XMPPService.this;
        }
    }

    private class MAMHandler
            implements MessageArchiveManagementModule.MessageArchiveItemReceivedEventHandler {

        private final Context context;

        public MAMHandler(XMPPService xmppService) {
            this.context = xmppService.getApplicationContext();
        }

        @Override
        public void onArchiveItemReceived(SessionObject sessionObject, String queryid,
                                          String messageId, Date timestamp,
                                          Message message) throws JaxmppException {
            final BareJID myJID = sessionObject.getUserBareJid();
            final JID msgFrom = message.getFrom();
            final JID msgTo = message.getTo();

            final JID chatJID;

            if (!myJID.equals(msgFrom.getBareJid())) {
                chatJID = msgFrom;
            } else if (!myJID.equals(msgTo.getBareJid())) {
                chatJID = msgTo;
            } else {
                Log.i(TAG, "Cannot process MAM message: " + message.getAsString());
                return;
            }

            Chat chat = getJaxmpp(sessionObject.getUserBareJid()).getModule(MessageModule.class).
                    getChatManager().getChat(chatJID, message.getThread());

            if (chat == null) {
                Log.d(TAG, "No chat found!");
                chat = getJaxmpp(sessionObject.getUserBareJid()).getModule(MessageModule.class)
                        .createChat(message.getFrom());
            }

            boolean stored = storeMessage(sessionObject, chat, message, timestamp);
        }
    }

    private class MessageHandler
            implements MessageModule.MessageReceivedHandler,
            MessageCarbonsModule.CarbonReceivedHandler,
            ChatStateExtension.ChatStateChangedHandler {

        private final Context context;

        private final MarkAsRead markAsRead;

        public MessageHandler(XMPPService xmppService) {
            this.context = xmppService.getApplicationContext();
            this.markAsRead = new MarkAsRead(context);
        }

        @Override
        public void onCarbonReceived(SessionObject sessionObject,
                                     MessageCarbonsModule.CarbonEventType carbonType,
                                     tigase.jaxmpp.core.client.xmpp.stanzas.Message msg,
                                     Chat chat) {
            try {
                boolean stored = storeMessage(sessionObject, chat, msg);
                if (stored && carbonType == MessageCarbonsModule.CarbonEventType.sent) {
                    markAsRead.markChatAsRead(chat.getId(), sessionObject.getUserBareJid(),
                            chat.getJid());
                }
            } catch (Exception ex) {
                Log.e(TAG, "Exception handling received carbon message", ex);
            }

            Intent i = new Intent(XMPPService.LAST_ACCOUNT_ACTIVITY);
            i.putExtra("account", sessionObject.getUserBareJid().toString());
            context.sendBroadcast(i);
        }

        @Override
        public void onChatStateChanged(SessionObject sessionObject, Chat chat, ChatState state) {
            try {
                Log.v(TAG,
                        "received chat state chaged event for " + chat.getJid().toString() +
                                ", new state = " + state);
                Uri uri = chat != null
                        ? ContentUris
                        .withAppendedId(TigaseSdk.getInstance().getOpenChatsUri(), chat.getId())
                        : TigaseSdk.getInstance().getOpenChatsUri();
                TigaseSdk.getInstance().getApplicationContext().getContentResolver()
                        .notifyChange(uri, null);
            } catch (Exception ex) {
                Log.e(TAG, "Exception handling received chat state change event", ex);
            }
        }

        @Override
        public void onMessageReceived(SessionObject sessionObject, Chat chat,
                                      tigase.jaxmpp.core.client.xmpp.stanzas.Message msg) {
            try {
                boolean stored = storeMessage(sessionObject, chat, msg);
                if (!stored) {
                    HyperLog.getInstance().e(TAG, "onMessageReceived", "Not stored!");
                }
            } catch (Exception ex) {
                Log.e(TAG, "Exception handling received message", ex);
            }

            Intent i = new Intent(XMPPService.LAST_ACCOUNT_ACTIVITY);
            i.putExtra("account", sessionObject.getUserBareJid().toString());
            context.sendBroadcast(i);
        }
    }

    private class MucHandler
            implements MucModule.MucMessageReceivedHandler,
            MucModule.YouJoinedHandler,
            MucModule.MessageErrorHandler,
            MucModule.StateChangeHandler,
            MucModule.PresenceErrorHandler,
            MucModule.OccupantLeavedHandler,
            MucModule.NewRoomCreatedHandler,
            MucModule.InvitationReceivedHandler {

        @Override
        public void onMessageError(SessionObject sessionObject,
                                   tigase.jaxmpp.core.client.xmpp.stanzas.Message msg,
                                   Room room, String nickname, Date timestamp) {
            try {
                Log.e(TAG,
                        "Error from room " + room.getRoomJid() + ", error = " + msg.getAsString());
            } catch (XMLException e) {
            }
            onMucMessageReceived(sessionObject, msg, room, nickname, timestamp);
        }

        @Override
        public void onMucMessageReceived(SessionObject sessionObject,
                                         tigase.jaxmpp.core.client.xmpp.stanzas.Message msg,
                                         Room room, String nickname,
                                         Date timestamp) {
            try {
                if (msg == null || msg.getBody() == null || room == null) {
                    return;
                }

                Log.d(TAG, "Received groupchat message: " + msg.getBody() + " Decoded as: " +
                        Crypto.decodeBody(msg.getBody()) + "; room=" + room);

                String body = Crypto.decodeBody(msg.getBody());

                ContentValues values = new ContentValues();
                values.put(DatabaseContract.ChatHistory.FIELD_JID, room.getRoomJid().toString());
//                values.put(DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME,
//                        ParseUserContact.getUsernameWithoutTag(nickname));
                values.put(DatabaseContract.ChatHistory.FIELD_AUTHOR_NICKNAME, nickname);
                values.put(DatabaseContract.ChatHistory.FIELD_TIMESTAMP, timestamp.getTime());
                values.put(DatabaseContract.ChatHistory.FIELD_STANZA_ID, msg.getId());
                values.put(DatabaseContract.ChatHistory.FIELD_CHAT_TYPE,
                        DatabaseContract.ChatHistory.CHAT_TYPE_MUC);

                boolean notify = false;

                if (msg.getType() == StanzaType.error) {
                    notify = true;
                    values.put(DatabaseContract.ChatHistory.FIELD_STATE,
                            DatabaseContract.ChatHistory.STATE_INCOMING);
                    values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
                            DatabaseContract.ChatHistory.ITEM_TYPE_ERROR);

                    ErrorElement error = ErrorElement.extract(msg);
                    body = "Error: ";
                    if (error != null) {
                        if (error.getText() != null) {
                            body += error.getText();
                        } else {
                            XMPPException.ErrorCondition errorCondition = error.getCondition();
                            if (errorCondition != null) {
                                body += errorCondition.getElementName();
                            }
                        }
                    }
                    if (msg.getBody() != null) {
                        body += "\n ------ \n";
                        body += Crypto.decodeBody(msg.getBody());
                    }

                    values.put(DatabaseContract.ChatHistory.FIELD_BODY, body);
                } else if (nickname != null && room.getNickname().equals(nickname)) {
                    values.put(DatabaseContract.ChatHistory.FIELD_STATE,
                            DatabaseContract.ChatHistory.STATE_OUT_DELIVERED);
                    values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
                            DatabaseContract.ChatHistory.ITEM_TYPE_MESSAGE);
                    values.put(DatabaseContract.ChatHistory.FIELD_BODY, body);
                } else if (nickname != null) {
                    values.put(DatabaseContract.ChatHistory.FIELD_STATE,
                            NotificationHandler.getInstance().isFocusedOnRoomId(room.getId())
                                    ? DatabaseContract.ChatHistory.STATE_INCOMING
                                    : DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD);
                    values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
                            DatabaseContract.ChatHistory.ITEM_TYPE_MESSAGE);
                    values.put(DatabaseContract.ChatHistory.FIELD_BODY, body);
                    notify = true;
                } else {
                    values.put(DatabaseContract.ChatHistory.FIELD_STATE,
                            NotificationHandler.getInstance().isFocusedOnRoomId(room.getId())
                                    ? DatabaseContract.ChatHistory.STATE_INCOMING
                                    : DatabaseContract.ChatHistory.STATE_INCOMING_UNREAD);
                    values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
                            DatabaseContract.ChatHistory.ITEM_TYPE_ERROR);
                    values.put(DatabaseContract.ChatHistory.FIELD_BODY, body);
                }

                String fileUrl = null;
                if (values.getAsInteger(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE) !=
                        DatabaseContract.ChatHistory.ITEM_TYPE_ERROR) {
                    Element geoloc = msg
                            .getChildrenNS("geoloc", "http://jabber.org/protocol/geoloc");
                    Element oobData = msg.getChildrenNS("x", "jabber:x:oob");
                    if (oobData != null) {
                        Element url = oobData.getFirstChild("url");
                        if (url != null) {
                            values.put(DatabaseContract.ChatHistory.FIELD_DATA, url.getAsString());
                            String mimeType = FileUploaderTask.guessMimeType(url.getValue());
                            if (mimeType != null && mimeType.startsWith("image/")) {
                                values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
                                        DatabaseContract.ChatHistory.ITEM_TYPE_IMAGE);
                            } else if (mimeType != null && mimeType.startsWith("video/")) {
                                values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
                                        DatabaseContract.ChatHistory.ITEM_TYPE_VIDEO);
                            } else {
                                values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
                                        DatabaseContract.ChatHistory.ITEM_TYPE_FILE);
                            }
                            fileUrl = url.getValue();
                        }
                    } else if (geoloc != null) {
                        values.put(DatabaseContract.ChatHistory.FIELD_DATA, geoloc.getAsString());
                        values.put(DatabaseContract.ChatHistory.FIELD_ITEM_TYPE,
                                DatabaseContract.ChatHistory.ITEM_TYPE_LOCALITY);
                    }
                }

                values.put(DatabaseContract.ChatHistory.FIELD_ACCOUNT,
                        sessionObject.getUserBareJid().toString());

                Uri uri = Uri.parse(TigaseSdk.getInstance().getMucHistoryUri() + "/" +
                        sessionObject.getUserBareJid() + "/" +
                        Uri.encode(room.getRoomJid().toString()));
                Uri msgUri = getContentResolver().insert(uri, values);

                if (msgUri != null && fileUrl != null) {
                    if (!FileDownloaderTask.isContentDownloaded(XMPPService.this, msgUri)) {
                        EventBus.getDefault().post(new DownloadFileEvent(fileUrl,
                                msg.getFrom().getLocalpart(), new DownloadedFileEvent(msgUri)));
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "Exception handling received MUC message", ex);
            }
        }

        @Override
        public void onNewRoomCreated(SessionObject sessionObject, Room room) {
            try {
                final Jaxmpp jaxmpp = getJaxmpp(sessionObject.getUserBareJid());
                MucModule module = jaxmpp.getModule(MucModule.class);

                module.getRoomConfiguration(room, new AsyncCallback() {
                    @Override
                    public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) {
                    }

                    @Override
                    public void onSuccess(Stanza responseStanza) throws JaxmppException {
                        Log.i("onsuccroom", responseStanza.getAsString());
                        String groupName = SharedPrefs.getRooms(getApplicationContext())
                                .get(room.getRoomJid().getLocalpart());
                        JabberDataElement j = new JabberDataElement(
                                responseStanza.getWrappedElement().getFirstChild().getFirstChild());
                        j.getField("muc#roomconfig_persistentroom").getFirstChild().setValue("1");
                        j.getField("muc#roomconfig_publicroom").getFirstChild().setValue("0");
                        j.getField("muc#roomconfig_membersonly").getFirstChild().setValue("1");
                        j.getField("muc#maxhistoryfetch").getFirstChild().setValue("500");
                        j.getField("muc#roomconfig_roomname").getFirstChild().setValue(groupName);

                        module.setRoomConfiguration(room, j, new AsyncCallback() {
                            @Override
                            public void onError(Stanza responseStanza,
                                                XMPPException.ErrorCondition error) throws JaxmppException {
                            }

                            @Override
                            public void onSuccess(Stanza responseStanza) throws JaxmppException {

                                for (JID j : DataCache.getInstance().getInvites()) {
                                    invite(room, j, "join", jaxmpp, sessionObject);

                                    IQ iq = IQ.create();
                                    iq.setFrom(JID.jidInstance(sessionObject.getUserBareJid()));
                                    iq.setTo(JID.jidInstance(room.getRoomJid()));
                                    iq.setId("admin1");
                                    iq.setType(StanzaType.set);

                                    Element q = ElementBuilder
                                            .create("query", "http://jabber.org/protocol/muc#admin")
                                            .getElement();
                                    iq.addChild(q);
                                    Element i = ElementBuilder.create("item").getElement();
                                    i.setAttribute("affiliation", Affiliation.owner.toString());
                                    i.setAttribute("jid", j.toString());
                                    iq.getQuery().addChild(i);
                                    jaxmpp.send(iq);
                                    EventBus.getDefault()
                                            .post(Constants.EVENT_BUS_UPDATE_CHAT_ADAPTER);
                                }
                            }

                            @Override
                            public void onTimeout() throws JaxmppException {

                            }
                        });
                    }

                    @Override
                    public void onTimeout() throws JaxmppException {

                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void invite(Room room, JID inviteeJID, String reason, Jaxmpp jaxmpp,
                           SessionObject sessionObject) throws JaxmppException {
            Message message = Message.create();
            message.setFrom(JID.jidInstance(sessionObject.getUserBareJid()));
            message.setTo(JID.jidInstance(room.getRoomJid()));
            message.setBody(Crypto.encodeBody(getString(R.string.room_join_text_message)));

            Element x = message.addChild(
                    ElementFactory.create("x", null, "http://jabber.org/protocol/muc#user"));
            Element invite = x.addChild(ElementFactory.create("invite"));
            invite.setAttribute("to", inviteeJID.toString());
            if (reason != null) {
                invite.addChild(ElementFactory.create("reason", reason, null));
            }
            jaxmpp.getContext().getWriter().write(message);

        }

        @Override
        public void onOccupantLeaved(SessionObject sessionObject, Room room, Occupant occupant,
                                     Presence presence,
                                     XMucUserElement xMucUserElement) {
            try {
                if (!occupant.getNickname().equals(room.getNickname()) ||
                        xMucUserElement.getStatuses() == null) {
                    return;
                }
                if (xMucUserElement.getStatuses().contains(301)) {
                    storeMucSysMsg(sessionObject, room, "You are banned from the room");
                } else if (xMucUserElement.getStatuses().contains(307)) {
                    storeMucSysMsg(sessionObject, room, "You are kicked from room");
                } else if (xMucUserElement.getStatuses().contains(321)) {
                    storeMucSysMsg(sessionObject, room,
                            "You are removed from the room because of an affiliation change");
                } else if (xMucUserElement.getStatuses().contains(322)) {
                    storeMucSysMsg(sessionObject, room,
                            "You are removed from the room because the room has been changed to members-only and you are not a member");
                } else if (xMucUserElement.getStatuses().contains(332)) {
                    storeMucSysMsg(sessionObject, room,
                            "You are removed from the room because the MUC service is being shut down");
                }

            } catch (XMLException e) {
                Log.e(TAG, "Exception handling", e);
            }
        }

        @Override
        public void onPresenceError(SessionObject sessionObject, Room room, Presence presence,
                                    String nickname) {
            Intent intent = new Intent();

            // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            // intent.setAction(MainActivity.ERROR_ACTION);
            intent.putExtra("account", sessionObject.getUserBareJid().toString());
            intent.putExtra("jid", "" + room.getRoomJid().toString());
            intent.putExtra("type", "muc");

            try {
                XMPPException.ErrorCondition c = presence.getErrorCondition();
                if (c != null) {
                    intent.putExtra("errorCondition", c.name());
                    intent.putExtra("errorMessage", c.name());
                } else {
                    intent.putExtra("errorCondition", "-");
                    intent.putExtra("errorMessage", "-");
                }
            } catch (XMLException ex) {
                ex.printStackTrace();
            }

            // if (focused) {
            // intent.setAction(ERROR_MESSAGE);
            // sendBroadcast(intent);
            // }
        }

        @Override
        public void onStateChange(SessionObject sessionObject, Room room,
                                  tigase.jaxmpp.core.client.xmpp.modules.muc.Room.State oldState,
                                  tigase.jaxmpp.core.client.xmpp.modules.muc.Room.State newState) {
            Log.v(TAG, "room " + room.getRoomJid() + " changed state from " + oldState + " to " +
                    newState);
            int state = CPresence.OFFLINE;
            switch (newState) {
                case joined:
                    state = CPresence.ONLINE;
                    break;
                default:
                    state = CPresence.OFFLINE;
            }
            chatProvider.updateRoomState(sessionObject, room.getRoomJid(), state);
        }

        @Override
        public void onYouJoined(SessionObject sessionObject, Room room, String asNickname) {
            // TODO Auto-generated method stub
            Log.v(TAG, "joined room " + room.getRoomJid() + " as " + asNickname);
            taskExecutor.execute(new SendUnsentGroupMessages(XMPPService.this,
                    getJaxmpp(sessionObject.getUserBareJid()), room));
            Jaxmpp jaxmpp = getJaxmpp(sessionObject.getUserBareJid());
            MucModule mucModule = jaxmpp.getModule(MucModule.class);
            try {
                mucModule.getRoomConfiguration(room, new AsyncCallback() {
                    @Override
                    public void onError(Stanza responseStanza, XMPPException.ErrorCondition error) {

                    }

                    @Override
                    public void onSuccess(Stanza responseStanza) throws JaxmppException {
                        JabberDataElement j = new JabberDataElement(
                                responseStanza.getWrappedElement().getFirstChild().getFirstChild());
                        String name = j.getField("muc#roomconfig_roomname").getFirstChild()
                                .getValue();
                        if (name != null && !name.equals("null"))
                            SharedPrefs.saveRooms(room.getRoomJid().getLocalpart(), name,
                                    getApplicationContext());
                        EventBus.getDefault().post(Constants.EVENT_BUS_UPDATE_CHAT_ADAPTER);
                    }

                    @Override
                    public void onTimeout() throws JaxmppException {

                    }
                });
            } catch (JaxmppException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onInvitationReceived(SessionObject sessionObject,
                                         MucModule.Invitation invitation,
                                         JID inviterJID, BareJID roomJID) {
            final Jaxmpp jaxmpp = getJaxmpp(sessionObject.getUserBareJid());
            MucModule mucModule = jaxmpp.getModule(MucModule.class);
            try {
                // TODO: check username
                mucModule.join(invitation, sessionObject.getUserBareJid().getLocalpart());
            } catch (JaxmppException e) {
                e.printStackTrace();
            }
        }
    }

    private class OwnPresenceFactoryImpl
            implements PresenceModule.OwnPresenceStanzaFactory {

        @Override
        public Presence create(SessionObject sessionObject) {
            try {
                Presence presence = Presence.create();

                SharedPreferences sharedPref = PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext());

                int defaultPresence = sharedPref.getInt("presence", CPresence.ONLINE);
                int presenceId = Long.valueOf(sharedPref.getLong("auto_presence", defaultPresence))
                        .intValue();

                PrioritiesEntity custPrio = sessionObject
                        .getUserProperty(CUSTOM_PRIORITIES_ENTITY_KEY);
                if (custPrio == null) {
                    custPrio = new PrioritiesEntity();
                }

                Log.d(TAG, "Before presence send. defaultPresence=" + defaultPresence +
                        "; presenceId=" + presenceId);

                switch (presenceId) {
                    case CPresence.OFFLINE:
                        presence.setType(StanzaType.unavailable);
                        break;
                    case CPresence.DND:
                        presence.setPriority(custPrio.getDnd());
                        presence.setShow(Presence.Show.dnd);
                        break;
                    case CPresence.XA:
                        presence.setPriority(custPrio.getXa());
                        presence.setShow(Presence.Show.xa);
                        break;
                    case CPresence.AWAY:
                        presence.setPriority(custPrio.getAway());
                        presence.setShow(Presence.Show.away);
                        break;
                    case CPresence.ONLINE:
                        presence.setPriority(custPrio.getOnline());
                        presence.setShow(Presence.Show.online);
                        break;
                    case CPresence.CHAT:
                        presence.setPriority(custPrio.getChat());
                        presence.setShow(Presence.Show.chat);
                        break;
                }

                return presence;
            } catch (JaxmppException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class PresenceHandler
            implements PresenceModule.ContactAvailableHandler,
            PresenceModule.ContactUnavailableHandler,
            PresenceModule.ContactChangedPresenceHandler,
            PresenceModule.ContactUnsubscribedHandler {

        private final XMPPService jaxmppService;

        public PresenceHandler(XMPPService jaxmppService) {
            this.jaxmppService = jaxmppService;
        }

        @Override
        public void onContactAvailable(SessionObject sessionObject, Presence stanza, JID jid,
                                       Presence.Show show,
                                       String status, Integer priority) throws JaxmppException {
            updateRosterItem(sessionObject, stanza);
            rosterProvider.updateStatus(sessionObject, jid);
        }

        @Override
        public void onContactChangedPresence(SessionObject sessionObject, Presence stanza, JID jid,
                                             Presence.Show show,
                                             String status,
                                             Integer priority) throws JaxmppException {
            updateRosterItem(sessionObject, stanza);
            rosterProvider.updateStatus(sessionObject, jid);
        }

        @Override
        public void onContactUnavailable(SessionObject sessionObject, Presence stanza, JID jid,
                                         String status) {
            try {
                updateRosterItem(sessionObject, stanza);
            } catch (JaxmppException ex) {
                Log.v(TAG, "Exception updating roster item presence", ex);
            }
            rosterProvider.updateStatus(sessionObject, jid);
        }

        @Override
        public void onContactUnsubscribed(SessionObject sessionObject, Presence stanza,
                                          BareJID jid) {
            try {
                updateRosterItem(sessionObject, stanza);
            } catch (JaxmppException ex) {
                Log.v(TAG, "Exception updating roster item presence", ex);
            }
            rosterProvider.updateStatus(sessionObject, JID.jidInstance(jid));
        }
    }

    private class RejoinToMucRooms
            implements Runnable {

        private final SessionObject sessionObject;

        public RejoinToMucRooms(SessionObject sessionObject) {
            this.sessionObject = sessionObject;
        }

        @Override
        public void run() {
            Log.i(TAG, "Rejoining to MUC Rooms. Account=" + sessionObject.getUserBareJid());
            try {
                Jaxmpp jaxmpp = multiJaxmpp.get(sessionObject);
                MucModule mucModule = jaxmpp.getModule(MucModule.class);
                for (Room room : mucModule.getRooms()) {

                    Log.d(TAG, "Room " + room.getRoomJid() + " is in state " + room.getState());
//					if (room.getState() != Room.State.joined) {
//						Log.d(TAG, "Rejoinning to " + room.getRoomJid());
//
//					} else {
//						taskExecutor.execute(new SendUnsentGroupMessages(room));
//					}
                    room.rejoin();
                }
            } catch (JaxmppException e) {
                Log.e(TAG, "Exception while rejoining to rooms on connect for account " +
                        sessionObject.getUserBareJid().toString());
            }
        }
    }

    private class ScreenStateReceiver
            extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Boolean screenOff = null;
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                screenOff = true;
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                screenOff = false;
            }
            if (screenOff != null) {
                sendAcks();
                mobileModeFeature.setMobileMode(screenOff);
            }
        }
    }
}
