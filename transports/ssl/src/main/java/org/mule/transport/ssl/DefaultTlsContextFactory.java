/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl;


import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.tls.TlsConfiguration;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.ssl.api.TlsContextFactory;
import org.mule.transport.ssl.api.TlsContextKeyStoreConfiguration;
import org.mule.transport.ssl.api.TlsContextTrustStoreConfiguration;
import org.mule.util.ArrayUtils;
import org.mule.util.FileUtils;
import org.mule.util.StringUtils;

import com.google.common.base.Joiner;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the {@code TlsContextFactory} interface, which delegates all its operations to a
 * {@code TlsConfiguration} object. Only enabled cipher suites and protocols will not delegate to it if configured.
 */
public class DefaultTlsContextFactory implements TlsContextFactory, Initialisable
{

    private static final Logger logger = LoggerFactory.getLogger(DefaultTlsContextFactory.class);
    private static final String DEFAULT = "default";

    private String name;

    private TlsConfiguration tlsConfiguration = new TlsConfiguration(null);

    private AtomicBoolean initialized = new AtomicBoolean(false);
    private boolean trustStoreInsecure = false;
    private String[] enabledProtocols;
    private String[] enabledCipherSuites;

    @Override
    public void initialise() throws InitialisationException
    {
        if (initialized.getAndSet(true))
        {
            return;
        }

        try
        {
            tlsConfiguration.initialise(null == getKeyStorePath(), null);
        }
        catch (CreateException e)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("Unable to initialise TLS configuration"), e, this);
        }

        if (!isUseDefaults(enabledProtocols))
        {
            String[] globalEnabledProtocols = tlsConfiguration.getEnabledProtocols();
            if (globalEnabledProtocols != null)
            {
                String[] validProtocols = ArrayUtils.intersection(enabledProtocols, globalEnabledProtocols);
                if (validProtocols.length < enabledProtocols.length)
                {
                    globalConfigNotHonored("protocols", globalEnabledProtocols);
                }
            }
        }

        if (!isUseDefaults(enabledCipherSuites))
        {
            String[] globalEnabledCipherSuites = tlsConfiguration.getEnabledCipherSuites();
            if (globalEnabledCipherSuites != null)
            {
                String[] validCipherSuites = ArrayUtils.intersection(enabledCipherSuites, globalEnabledCipherSuites);
                if (validCipherSuites.length < enabledCipherSuites.length)
                {
                    globalConfigNotHonored("cipher suites", globalEnabledCipherSuites);
                }
            }
        }
    }

    private boolean isUseDefaults(String[] array)
    {
        return (array == null) || ((array.length == 1) && DEFAULT.equalsIgnoreCase(array[0]));
    }

    private void globalConfigNotHonored(String element, String[] elementArray) throws InitialisationException
    {
        throw new InitialisationException(MessageFactory.createStaticMessage(String.format("Some selected %1$s are invalid. Valid %1$s according to your TLS configuration file are: %2$s", element, Joiner.on(", ").join(elementArray))), this);
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getKeyStorePath()
    {
        return tlsConfiguration.getKeyStore();
    }

    public void setKeyStorePath(String name) throws IOException
    {
        tlsConfiguration.setKeyStore(name);
    }

    public String getKeyStoreType()
    {
        return tlsConfiguration.getKeyStoreType();
    }

    public void setKeyStoreType(String keyStoreType)
    {
        tlsConfiguration.setKeyStoreType(keyStoreType);
    }

    public String getKeyAlias()
    {
        return tlsConfiguration.getKeyAlias();
    }

    public void setKeyAlias(String keyAlias)
    {
        tlsConfiguration.setKeyAlias(keyAlias);
    }

    public String getKeyStorePassword()
    {
        return tlsConfiguration.getKeyStorePassword();
    }

    public void setKeyStorePassword(String storePassword)
    {
        tlsConfiguration.setKeyStorePassword(storePassword);
    }

    public String getKeyManagerPassword()
    {
        return tlsConfiguration.getKeyPassword();
    }

    public void setKeyManagerPassword(String keyManagerPassword)
    {
        tlsConfiguration.setKeyPassword(keyManagerPassword);
    }

    public String getKeyManagerAlgorithm()
    {
        return tlsConfiguration.getKeyManagerAlgorithm();
    }

    public void setKeyManagerAlgorithm(String keyManagerAlgorithm)
    {
        tlsConfiguration.setKeyManagerAlgorithm(keyManagerAlgorithm);
    }

    public String getTrustStorePath()
    {
        return tlsConfiguration.getTrustStore();
    }

    public void setTrustStorePath(String trustStorePath) throws IOException
    {
        String trustStoreResource = FileUtils.getResourcePath(trustStorePath, getClass());
        if (trustStoreResource == null)
        {
            throw new IOException(String.format("Resource %s could not be found", trustStorePath));
        }
        tlsConfiguration.setTrustStore(trustStorePath);
    }

    public String getTrustStoreType()
    {
        return tlsConfiguration.getTrustStoreType();
    }

    public void setTrustStoreType(String trustStoreType)
    {
        tlsConfiguration.setTrustStoreType(trustStoreType);
    }

    public String getTrustStorePassword()
    {
        return tlsConfiguration.getTrustStorePassword();
    }

    public void setTrustStorePassword(String trustStorePassword)
    {
        tlsConfiguration.setTrustStorePassword(trustStorePassword);
    }

    public String getTrustManagerAlgorithm()
    {
        return tlsConfiguration.getTrustManagerAlgorithm();
    }

    public void setTrustManagerAlgorithm(String trustManagerAlgorithm)
    {
        tlsConfiguration.setTrustManagerAlgorithm(trustManagerAlgorithm);
    }

    public boolean isTrustStoreInsecure()
    {
        return trustStoreInsecure;
    }

    public void setTrustStoreInsecure(boolean insecure)
    {
        if (insecure)
        {
            logger.warn(String.format("TLS context %s trust store set as insecure. No certificate validations will be performed, rendering connections vulnerable to attacks. Use at own risk.", name == null ? StringUtils.EMPTY : name));
        }
        this.trustStoreInsecure = insecure;
    }

    @Override
    public SSLContext createSslContext() throws KeyManagementException, NoSuchAlgorithmException, CreateException
    {
        SSLContext sslContext;
        if (trustStoreInsecure)
        {
            sslContext = tlsConfiguration.getSslContext(new TrustManager[]{new InsecureTrustManager()});
        }
        else
        {
            sslContext = tlsConfiguration.getSslContext();
        }
        return sslContext;
    }

    @Override
    public String[] getEnabledCipherSuites()
    {
        if (isUseDefaults(enabledCipherSuites))
        {
            return tlsConfiguration.getEnabledCipherSuites();
        }
        else
        {
            return enabledCipherSuites;
        }
    }

    public void setEnabledCipherSuites(String enabledCipherSuites)
    {
        this.enabledCipherSuites = StringUtils.splitAndTrim(enabledCipherSuites, ",");
    }

    @Override
    public String[] getEnabledProtocols()
    {
        if (isUseDefaults(enabledProtocols))
        {
            return tlsConfiguration.getEnabledProtocols();
        }
        else
        {
            return enabledProtocols;
        }
    }

    public void setEnabledProtocols(String enabledProtocols)
    {
        this.enabledProtocols = StringUtils.splitAndTrim(enabledProtocols, ",");
    }

    public Boolean getRcStandardEnable()
    {
        return tlsConfiguration.getRcStandardEnable();
    }

    public void setRcStandardEnable(Boolean rcStandardEnable)
    {
        tlsConfiguration.setRcStandardEnable(rcStandardEnable);
    }

    public Boolean getRcStandardOnlyEndEntities()
    {
        return tlsConfiguration.getRcStandardOnlyEndEntities();
    }

    public void setRcStandardOnlyEndEntities(Boolean rcStandardOnlyEndEntities)
    {
        tlsConfiguration.setRcStandardOnlyEndEntities(rcStandardOnlyEndEntities);
    }

    public Boolean getRcStandardPreferCrls()
    {
        return tlsConfiguration.getRcStandardPreferCrls();
    }

    public void setRcStandardPreferCrls(Boolean rcStandardPreferCrls)
    {
        tlsConfiguration.setRcStandardPreferCrls(rcStandardPreferCrls);
    }

    public Boolean getRcStandardNoFallback()
    {
        return tlsConfiguration.getRcStandardNoFallback();
    }

    public void setRcStandardNoFallback(Boolean rcStandardNoFallback)
    {
        tlsConfiguration.setRcStandardNoFallback(rcStandardNoFallback);
    }

    public Boolean getRcStandardSoftFail()
    {
        return tlsConfiguration.getRcStandardSoftFail();
    }

    public void setRcStandardSoftFail(Boolean rcStandardSoftFail)
    {
        tlsConfiguration.setRcStandardSoftFail(rcStandardSoftFail);
    }

    public String getRcCrlFilePath()
    {
        return tlsConfiguration.getRcCrlFilePath();
    }

    public void setRcCrlFilePath(String rcCrlFilePath)
    {
        tlsConfiguration.setRcCrlFilePath(rcCrlFilePath);
    }

    public String getRcCustomOcspUrl()
    {
        return tlsConfiguration.getRcCustomOcspUrl();
    }

    public void setRcCustomOcspUrl(String rcCustomOcspUrl)
    {
        tlsConfiguration.setRcCustomOcspUrl(rcCustomOcspUrl);
    }

    public String getRcCustomOcspCertAlias()
    {
        return tlsConfiguration.getRcCustomOcspCertAlias();
    }

    public void setRcCustomOcspCertAlias(String rcCustomOcspCertAlias)
    {
        tlsConfiguration.setRcCustomOcspCertAlias(rcCustomOcspCertAlias);
    }

    @Override
    public boolean isKeyStoreConfigured()
    {
        return tlsConfiguration.getKeyStore() != null;
    }

    @Override
    public boolean isTrustStoreConfigured()
    {
        return tlsConfiguration.getTrustStore() != null;
    }

    @Override
    public TlsContextKeyStoreConfiguration getKeyStoreConfiguration()
    {
        return new TlsContextKeyStoreConfiguration()
        {
            @Override
            public String getAlias()
            {
                return getKeyAlias();
            }

            @Override
            public String getKeyPassword()
            {
                return getKeyManagerPassword();
            }

            @Override
            public String getPath()
            {
                return getKeyStorePath();
            }

            @Override
            public String getPassword()
            {
                return getKeyStorePassword();
            }

            @Override
            public String getType()
            {
                return getKeyStoreType();
            }

            @Override
            public String getAlgorithm()
            {
                return getKeyManagerAlgorithm();
            }
        };
    }

    @Override
    public TlsContextTrustStoreConfiguration getTrustStoreConfiguration()
    {
        return new TlsContextTrustStoreConfiguration()
        {
            @Override
            public String getPath()
            {
                return getTrustStorePath();
            }

            @Override
            public String getPassword()
            {
                return getTrustStorePassword();
            }

            @Override
            public String getType()
            {
                return getTrustStoreType();
            }

            @Override
            public String getAlgorithm()
            {
                return getTrustManagerAlgorithm();
            }

            @Override
            public boolean isInsecure()
            {
                return isTrustStoreInsecure();
            }
        };
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof DefaultTlsContextFactory))
        {
            return false;
        }

        DefaultTlsContextFactory that = (DefaultTlsContextFactory) o;

        if (!tlsConfiguration.equals(that.tlsConfiguration))
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return tlsConfiguration.hashCode();
    }

    private static class InsecureTrustManager implements X509TrustManager
    {

        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[0];
        }

        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }
    }
}
