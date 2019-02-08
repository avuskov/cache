package auskov;

import java.io.IOException;

public class MyCacheSimpleImplTest extends MyCacheTest {
    @Override
    protected MyCache createANewCache() throws IOException {
        return MyCacheSimpleImpl.createCash();
    }
}