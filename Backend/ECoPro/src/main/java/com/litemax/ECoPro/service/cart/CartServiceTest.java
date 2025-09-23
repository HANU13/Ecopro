package com.litemax.ECoPro.service.cart;



//@ExtendWith(MockitoExtension.class)
class CartServiceTest {

//    @Mock
//    private CartRepository cartRepository;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private ProductRepository productRepository;
//
//    private CartService cartService;
//
//    @BeforeEach
//    void setUp() {
//        cartService = new CartService(cartRepository, null, userRepository, productRepository, null);
//    }
//
//    @Test
//    void shouldCreateCartForNewUser() {
//        // Given
//        String userEmail = "test@example.com";
//        User user = new User();
//        user.setId(1L);
//        user.setEmail(userEmail);
//
//        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
//        when(cartRepository.findByUserIdAndStatus(eq(1L), eq(Cart.CartStatus.ACTIVE)))
//                .thenReturn(Optional.empty());
//        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
//            Cart cart = invocation.getArgument(0);
//            cart.setId(1L);
//            return cart;
//        });
//
//        // When
//        CartResponse result = cartService.getOrCreateCart(userEmail);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(1L, result.getUserId());
//        assertEquals("ACTIVE", result.getStatus());
//        verify(cartRepository).save(any(Cart.class));
//    }
//
//    @Test
//    void shouldAddItemToCart() {
//        // Given
//        String userEmail = "test@example.com";
//        User user = new User();
//        user.setId(1L);
//        
//        Cart cart = new Cart();
//        cart.setId(1L);
//        cart.setUser(user);
//
//        Product product = new Product();
//        product.setId(1L);
//        product.setName("Test Product");
//        product.setPrice(new BigDecimal("29.99"));
//        product.setStatus(Product.ProductStatus.ACTIVE);
//        product.setTrackInventory(false);
//
//        CartRequest request = new CartRequest();
//        request.setProductId(1L);
//        request.setQuantity(2);
//
//        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
//        when(cartRepository.findByUserIdAndStatus(1L, Cart.CartStatus.ACTIVE))
//                .thenReturn(Optional.of(cart));
//        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
//        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
//
//        // When
//        CartResponse result = cartService.addItemToCart(userEmail, request);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(1, result.getItemsCount());
//        verify(cartRepository).save(cart);
//    }
}