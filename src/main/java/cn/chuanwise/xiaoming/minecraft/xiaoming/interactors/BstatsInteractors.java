package cn.chuanwise.xiaoming.minecraft.xiaoming.interactors;

import cn.chuanwise.toolkit.map.TypePathGetter;
import cn.chuanwise.toolkit.serialize.Serializer;
import cn.chuanwise.toolkit.serialize.json.JacksonSerializer;
import cn.chuanwise.util.Preconditions;
import cn.chuanwise.util.Streams;
import cn.chuanwise.xiaoming.annotation.Filter;
import cn.chuanwise.xiaoming.interactor.SimpleInteractors;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.util.Words;
import cn.chuanwise.xiaoming.user.XiaoMingUser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class BstatsInteractors extends SimpleInteractors<XMMCXiaoMingPlugin> {
    private String request(String urlString, long delay, long timeout, Charset charset) throws IOException, InterruptedException {
        Preconditions.argumentNonEmpty(urlString);

        final URL url = new URL(urlString);
        try (InputStream inputStream = url.openStream()) {
            // waiting for writing finished
            Thread.sleep(delay);

            return Streams.read(inputStream, charset);
        }
    }

    private String request(String urlString) throws IOException, InterruptedException {
        return request(urlString, TimeUnit.MILLISECONDS.toMillis(500), plugin.getSessionConfiguration().getConnection().getResponseTimeout(), StandardCharsets.UTF_8);
    }

    @Filter(Words.XIAOMING + Words.MINECRAFT + Words.USER + Words.STATISTIC)
    void bstatsInfo(XiaoMingUser user) throws IOException, InterruptedException {
        final int pluginId = 12125;

        final Serializer serializer = new JacksonSerializer();
        final TypePathGetter deserialize = serializer.deserialize(request("https://bstats.org/api/v1/plugins/" + pluginId + "/charts/players/data/?maxElements=4320"));
    }
}
