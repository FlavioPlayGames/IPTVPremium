import ctypes as C, ctypes.util, os, math, sys, re
from pathlib import Path
os.environ['EGL_PLATFORM']='surfaceless'
os.environ.setdefault('MESA_SHADER_CACHE_DIR','/tmp/iptv-mesa-cache')
egl=C.CDLL(ctypes.util.find_library('EGL'))
def ef(n,ret,args):
 f=getattr(egl,n);f.restype=ret;f.argtypes=args;return f
ptr=C.c_void_p; I=C.c_int; U=C.c_uint; F=C.c_float
getproc=ef('eglGetProcAddress',ptr,[C.c_char_p])
getdisplay=ef('eglGetDisplay',ptr,[ptr]);d=getdisplay(None)
major=I();minor=I();assert ef('eglInitialize',U,[ptr,C.POINTER(I),C.POINTER(I)])(d,C.byref(major),C.byref(minor))
assert ef('eglBindAPI',U,[U])(0x30A0)
attrs=(I*15)(0x3033,1,0x3040,4,0x3024,8,0x3023,8,0x3022,8,0x3021,8,0x3038,0x3038,0x3038)
config=ptr();num=I();assert ef('eglChooseConfig',U,[ptr,C.POINTER(I),C.POINTER(ptr),I,C.POINTER(I)])(d,attrs,C.byref(config),1,C.byref(num)) and num.value
surface=ef('eglCreatePbufferSurface',ptr,[ptr,ptr,C.POINTER(I)])(d,config,(I*5)(0x3057,256,0x3056,256,0x3038))
context=ef('eglCreateContext',ptr,[ptr,ptr,ptr,C.POINTER(I)])(d,config,None,(I*3)(0x3098,2,0x3038));assert context
assert ef('eglMakeCurrent',U,[ptr,ptr,ptr,ptr])(d,surface,surface,context)
def gl(n,ret,args):
 address=getproc(n.encode());assert address,n
 return C.CFUNCTYPE(ret,*args)(address)
glGetString=gl('glGetString',C.c_char_p,[U]);print('Renderer:',glGetString(0x1F01).decode())
createShader=gl('glCreateShader',U,[U]);shaderSource=gl('glShaderSource',None,[U,I,C.POINTER(C.c_char_p),ptr]);compileShader=gl('glCompileShader',None,[U]);shaderiv=gl('glGetShaderiv',None,[U,U,C.POINTER(I)]);shaderlog=gl('glGetShaderInfoLog',None,[U,I,ptr,ptr]);createProgram=gl('glCreateProgram',U,[]);attach=gl('glAttachShader',None,[U,U]);link=gl('glLinkProgram',None,[U]);programiv=gl('glGetProgramiv',None,[U,U,C.POINTER(I)]);programlog=gl('glGetProgramInfoLog',None,[U,I,ptr,ptr]);use=gl('glUseProgram',None,[U]);getuniform=gl('glGetUniformLocation',I,[U,C.c_char_p]);u1i=gl('glUniform1i',None,[I,I]);u1f=gl('glUniform1f',None,[I,F]);u2f=gl('glUniform2f',None,[I,F,F]);getattrloc=gl('glGetAttribLocation',I,[U,C.c_char_p]);vertex=gl('glVertexAttribPointer',None,[U,I,U,U,I,ptr]);enable=gl('glEnableVertexAttribArray',None,[U]);gentex=gl('glGenTextures',None,[I,C.POINTER(U)]);bindtex=gl('glBindTexture',None,[U,U]);texparam=gl('glTexParameteri',None,[U,U,I]);teximage=gl('glTexImage2D',None,[U,I,I,I,I,I,U,U,ptr]);active=gl('glActiveTexture',None,[U]);genfb=gl('glGenFramebuffers',None,[I,C.POINTER(U)]);bindfb=gl('glBindFramebuffer',None,[U,U]);fbtex=gl('glFramebufferTexture2D',None,[U,U,U,U,I]);fbstatus=gl('glCheckFramebufferStatus',U,[U]);viewport=gl('glViewport',None,[I,I,I,I]);draw=gl('glDrawArrays',None,[U,I,I]);read=gl('glReadPixels',None,[I,I,I,I,U,U,ptr]);error=gl('glGetError',U,[])
root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path.cwd()
r=root/'app/src/main/assets/shaders'
source=(root/'app/src/main/java/br/com/flaviotorres/iptvpremium/core/navigation/player/PlaybackImageEffects.kt').read_text()
mode=re.search(r'glDrawArrays\(GLES20\.(GL_TRIANGLE_\w+)',source).group(1)
draw_mode={'GL_TRIANGLE_STRIP':0x0005,'GL_TRIANGLE_FAN':0x0006}[mode]
def shader(kind,s):
 sh=createShader(kind);value=C.c_char_p(s.encode());shaderSource(sh,1,C.byref(value),None);compileShader(sh);ok=I();shaderiv(sh,0x8B81,C.byref(ok))
 if not ok.value:
  log=C.create_string_buffer(12000);shaderlog(sh,12000,None,log);raise AssertionError(log.value.decode())
 return sh
programs={}
for name in ['copy','rcas','easu']:
 p=createProgram();attach(p,shader(0x8B31,(r/'image_vertex.glsl').read_text()));attach(p,shader(0x8B30,(r/f'image_{name}.glsl').read_text()));link(p);ok=I();programiv(p,0x8B82,C.byref(ok))
 if not ok.value:
  log=C.create_string_buffer(12000);programlog(p,12000,None,log);raise AssertionError(log.value.decode())
 programs[name]=p;print('PASS: compiled and linked',name)
def texture(w,h,data=None):
 t=U();gentex(1,C.byref(t));bindtex(0x0DE1,t);texparam(0x0DE1,0x2801,0x2601);texparam(0x0DE1,0x2800,0x2601);texparam(0x0DE1,0x2802,0x812F);texparam(0x0DE1,0x2803,0x812F)
 buf=(C.c_ubyte*len(data)).from_buffer_copy(data) if data is not None else None
 teximage(0x0DE1,0,0x1908,w,h,0,0x1908,0x1401,buf);return t
# Actual Media3 1.11.1 GlUtil.getNormalizedCoordinateBounds() order.
quad=(F*16)(-1,-1,0,1, 1,-1,0,1, -1,1,0,1, 1,1,0,1)
def render(name,source,w,h,ow,oh,strength=.35):
 output=texture(ow,oh);fb=U();genfb(1,C.byref(fb));bindfb(0x8D40,fb);fbtex(0x8D40,0x8CE0,0x0DE1,output,0);assert fbstatus(0x8D40)==0x8CD5
 p=programs[name];use(p);active(0x84C0);bindtex(0x0DE1,source);u1i(getuniform(p,b'uTexSampler'),0);u2f(getuniform(p,b'uInputSize'),w,h);u1f(getuniform(p,b'uStrength'),strength)
 gl('glClearColor',None,[F,F,F,F])(0,0,0,0);gl('glClear',None,[U])(0x4000)
 loc=getattrloc(p,b'aPosition');vertex(loc,4,0x1406,0,0,quad);enable(loc);viewport(0,0,ow,oh);draw(draw_mode,0,4);assert error()==0
 pixels=(C.c_ubyte*(ow*oh*4))();read(0,0,ow,oh,0x1908,0x1401,pixels);assert error()==0
 return output,bytes(pixels)
cases=0
for rgb in [(0,0,0),(255,255,255),(64,128,192),(2,3,4),(128,128,128)]:
 data=bytes((*rgb,255))*32*32;source=texture(32,32,data)
 for strength in [.35,.55,.75]:
  _,light=render('rcas',source,32,32,32,32,strength)
  expanded,fsr=render('easu',source,32,32,48,48)
  _,fsr=render('rcas',expanded,48,48,48,48,strength)
  for out in [light,fsr]:
   assert all(abs(out[i]-rgb[i%4])<=2 for i in range(len(out)) if i%4!=3),rgb
   assert all(out[i]==255 for i in range(3,len(out),4))
  cases+=1
print('PASS:',cases,'constant-color cases in both modes, including black/white, preserve color and opaque alpha')
# A non-uniform edge must actually change under sharpening; this rejects a fake pass-through.
data=bytes(v for y in range(32) for x in range(32) for v in ((64 if x<15 else 128 if x==15 else 192),)*3+(255,))
source=texture(32,32,data);_,light=render('rcas',source,32,32,32,32,.75);assert light!=data
print('PASS: RCAS changes edge contrast rather than returning an unchanged image')
print('All OpenGL ES checks passed; this is a software renderer, not a Mali performance measurement.')

source=texture(32,32,data)
_,bilinear=render('copy',source,32,32,48,48)
_,upscaled=render('easu',source,32,32,48,48)
assert upscaled!=bilinear
print('PASS: EASU upscaling differs from bilinear on an edge pattern')
quad_data=bytes(v for y in range(32) for x in range(32) for v in ((255 if x<16 else 0,255 if y<16 else 0,128,255)))
source=texture(32,32,quad_data);tex,up=render('easu',source,32,32,48,48);_,out=render('rcas',tex,48,48,48,48)
for x,y in [(2,2),(45,2),(2,45),(45,45)]:
 expected=(255 if x<24 else 0,255 if y<24 else 0,128,255)
 actual=out[(y*48+x)*4:(y*48+x)*4+4]
 assert all(abs(a-b)<=2 for a,b in zip(actual,expected)),(actual,expected)
print('PASS: two-pass FSR preserves color quadrants and image orientation')
