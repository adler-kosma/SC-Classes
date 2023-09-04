
///////////Ljudkälla////////////////////
//aBus -- gator main
//bBus -- gator slave
//vBus -- gator vocal
//outBus -- no gator
//vocBus -- vocal to stereo out through noGator
///////////RoutingSynth////////////////////
//vocBusOut
//panBus -- skickar ljud till stereo genom \outSynth
//panBusC -- skickar ljud till \pannerCircle
//panBusD -- skickar ljud till \discreteOut som spelas av en Pbindef(\panning)

Konsert1 {
	var server;
	var synth, device, <synthGroup, <fxGroup, <panGroup, <voiceBuf, <a1Bus, <a2Bus, <outBus1, <outBus2, <bBus, <vBus, <sawBus, <outBus, <vocBus, <vocBusOut, <revBus, <panBusSaw, <panBus, <panBusC, <panBusD, <midiBus, delBus, masterBus, masterSynth, gator_s, out_s, korg_s, moog_s, mic_s, klank_s, reverb_s, delay_s, panner_s;
	var fxAssignments;

	*new { //klassmetod
		^super.new.initKonsert1;
	}

	initKonsert1 { //instansmetod
		server = Server.default;
		MIDIClient.init;
		server.options.device = "UltraLite-mk4";
		try {
			midiBus = MIDIOut.new(1);
			CmdPeriod.add({(0..127).do{arg n; midiBus.noteOff(0,n)}});
		} {
			server.options.device = "Built-in"
		};

		server.options.sampleRate = 48000;

		server.waitForBoot{
			Konsert1.sendSynthDefs(server.options.device);
			server.sync;
			//~bdkrev = Buffer.readChannel(server,"/Users/adele21/Documents/SuperCollider/klangwürfel-lead you home/lead_you_home_rev_rec_1.aiff", channels:[0]);
			//~perc = Buffer.readChannel(server,"/Users/adele21/Documents/SuperCollider/Mobilen/ljuden/12-Wood_Block.wav", channels: [0]);
			//~kick = Buffer.readChannel(server,"/Users/adele21/Music/DRUMS/kicks från daniel/kick(mmm).wav", channels: [0]);
			//~lowF = Buffer.readChannel(server,"/Users/adele21/Music/Logic/lowF_1.wav", channels: [0]);
			voiceBuf = Buffer.alloc(server, server.sampleRate * 90, 1);
			a1Bus = Bus.audio(server, 1); // master1
			a2Bus = Bus.audio(server, 1); // master2
			outBus1 = Bus.audio(server, 1); // master1 out
			outBus2 = Bus.audio(server, 1); // master2 out
			bBus = Bus.audio(server, 1); // slave
			vBus = Bus.audio(server, 1); // vocals to gator
			sawBus = Bus.audio(server, 1); // saw to reverb
			vocBus = Bus.audio(server, 1); // vocals pass gator
			vocBusOut = Bus.audio(server, 1); //vocals out stereo
			revBus = Bus.audio(server, 1); //reverb
			panBusSaw = Bus.audio(server, 1); //panning for saw
			panBus = Bus.audio(server, 1); //panning to all channels
			panBusC = Bus.audio(server, 1); //panning circle
			panBusD = Bus.audio(server, 1); //panning discrete
			outBus = Bus.audio(server, 1); //routing passed gator through reverb
			delBus = Bus.audio(server, 1); //delay
			masterBus  = Bus.audio(server, 4);
			synthGroup = Group.new(server);
			fxGroup = Group.after(synthGroup);
			panGroup = Group.after(fxGroup);
			masterSynth = Synth.after(panGroup, \master, [\inBus, 0]);
			server.options.numOutputBusChannels = 10;
			server.options.numInputBusChannels = 4;
			server.recChannels = 8;
		};
	}

	gator {|parValEvent|
		fork{
			if(gator_s.isNil, {
				gator_s = Synth(\gator, nil, fxGroup, 'addToHead');
				server.sync;
				parValEvent.keysValuesDo{|par, val|
					gator_s.set(par, val);
				};
			}, {
				parValEvent.keysValuesDo{|par, val|
					gator_s.set(par, val);
				};
			});
		};
	}

	*sendSynthDefs {|device|
		(
			if(device == "UltraLite-mk4", {
				SynthDef.new(\mic,{
					arg inBus, outBus, amp=1, freq=293.67, modMix=0;
					var input, sig, rmod;
					input = SoundIn.ar(inBus) * amp;
					rmod = input * SinOsc.ar(freq);
					sig = input + (rmod * modMix);
					sig = HPF.ar(sig, 400);
					Out.ar(outBus, sig);
				}).add;

				SynthDef.new(\moog, {
					arg inBus, outBus, amp=1;
					var input, filter, filter0;
					input = SoundIn.ar(inBus) * amp;
					filter = BBandStop.ar(input, 400, 2);
					filter0 = LPF.ar(filter, 1000);
					Out.ar(outBus, filter0);
				}).add;

				SynthDef.new(\D1200,{
					arg inBus, outBus, amp=1;
					var input;
					input = SoundIn.ar(inBus) * amp;
					Out.ar(outBus, input);
				}).add;
			});

			SynthDef(\klank, {
				arg outBus, i_freq=200, atk=0.1, /*sus=1, */rel=0.6, amp1=0.5, val=466.16;
				var klank, n, harm, amp, ring, env;
				env = EnvGen.ar(Env.perc(atk, rel), doneAction:2);/*
				Env([0,1,0],[atk,sus,rel]), gate,
				doneAction:2);*/
				harm = \harm.ir(Array.series(4, 1, 1));
				amp = \amp.ir(Array.fill(4, 0.05));
				ring = \ring.ir(Array.fill(4, 1));
				klank = Klank.ar(`[harm, amp, ring], {SinOsc.ar(val)*0.03}.dup, i_freq) * env * amp1;
				Out.ar(outBus, klank);
			}).add;

			SynthDef(\bufSynth, {
				arg outBus, buf, rate=1, start=0, amp=1, atk=0.1, rel=0.5;
				var sig, env;
				env = EnvGen.kr(Env.perc(atk, rel), doneAction:2);
				sig = PlayBuf.ar(1, buf, rate, 1, start);
				sig = sig * amp * env;
				Out.ar(outBus, sig);
			}).add;

			SynthDef(\record, {
				arg outBus=0, inChannel=0, bufNum;
				var input;
				input = RecordBuf.ar(SoundIn.ar(inChannel), bufNum, doneAction: 2);
				Out.ar(outBus, 0);
			}).add;

			SynthDef(\playBackBuf, {
				arg outBus=0, bufNum, start=0, done=2, freq=800, loop=1, pos=(-1), centerFreq=2000, bw=1, amp=1;
				var buffer, outSig;
				buffer = PlayBuf.ar(1, bufNum, BufRateScale.kr(bufNum), 1, start, loop, doneAction:done) * amp;
				buffer = BBandPass.ar(buffer, centerFreq, bw);
				Out.ar(outBus, buffer);
			}).add;

			SynthDef(\brownNoiz0, {
				arg freq, amp=0.01, rel=1.0;
				var sig, env;
				env = EnvGen.kr(Env.perc(0.1, rel), doneAction:2);
				sig = BrownNoise.ar(amp) * env;
				Out.ar(freq, sig);
			}).add;

			SynthDef(\sin, {
				arg freq=200, amp=0.01, rel=1.0, outBus;
				var sig, env;
				sig = SinOsc.ar(freq) * amp;
				Out.ar(outBus, sig);
			}).add;

			SynthDef(\brownNoiz1, {
				arg outBus, amp=0.01, rel=1.0;
				var sig, env;
				env = EnvGen.kr(Env.perc(0.1, rel), doneAction:2);
				sig = BrownNoise.ar(amp) * env;
				Out.ar(outBus, sig);
			}).add;

			SynthDef(\saw, {
				arg gate=1, amp=0.01, freq=440, atk=0.1, sus=1, rel=0.5, freqF=800, outBus;
				var env, sig, filter;
				env = EnvGen.kr(Env.asr(atk, sus, rel), gate, doneAction:2);
				sig = Saw.ar(freq) * amp;
				filter = LPF.ar(sig, freqF);
				sig = filter * env;
				Out.ar(outBus, sig);
			}).add;

			SynthDef(\grainBuf, {
				arg outBus, buf, lag=10, minFreq=1.0, maxFreq=6.0, minRate=0.5,maxRate=1.4, amp=0.3, lineDur=10;
				var sig, trig, rate, freq, lineF, mixS;
				lineF = XLine.kr(0.1, 1, lineDur);
				freq = LFNoise1.ar(1).range(minFreq.lag(lag),maxFreq.lag(lag)) * lineF;
				trig = Impulse.kr(freq);
				rate = LFNoise0.kr(200).range(minRate.lag(lag), maxRate.lag(lag));
				sig = PlayBuf.ar(1, buf, rate, trig) * amp.lag(lag);
				Out.ar(outBus, sig);
			}).add;

			SynthDef(\gator,  {
				arg inBusA1, inBusA2, inBusB, gate=1, lag=10, clampTime=0.01, relaxTime=0.1, thresh=0.5,
				slopeBelow=3, slopeAbove=1, outBus1, outBus2, revOut, delOut, revAmp=0.5;
				var kick1, kick2, pads, vocal, snd1, snd2, env, outSig, outSig1, outSig2;
				env = EnvGen.kr(Env.asr(), gate, doneAction: 2);
				kick1 = In.ar(inBusA1, 1);
				kick2 = In.ar(inBusA2, 1);
				pads = In.ar(inBusB, 1);
				snd1 = Compander.ar(pads, kick1, thresh, slopeBelow, slopeAbove, clampTime.lag(lag), relaxTime.lag(lag));
				snd2 = Compander.ar(pads, kick2, thresh, slopeBelow, slopeAbove, clampTime.lag(lag), relaxTime.lag(lag));
				outSig1 = (kick1 + snd1) * env;
				outSig2 = (kick2 + snd2) * env;
				outSig = (outSig1 + outSig2);
				Out.ar(outBus1, outSig1);
				Out.ar(outBus2, outSig2);
				Out.ar(revOut, outSig*revAmp);
			}).add;

			SynthDef(\reverb, {
				arg inBus, outBus, level=0.1, gate=1;
				var input, env, rev;
				env = EnvGen.ar(Env.asr(), gate, doneAction: 2);
				input = In.ar(inBus, 1) * env * level.lag(10);
				input = Pan2.ar(input);
				rev = NHHall.ar(input, 5, 0.5, 200, 0.5, 4000, 0.1, 0.5, 0.5, 0.2, 0.3);
				Out.ar(outBus, rev);
				//Out.ar(2, rev*0.4);
				//Out.ar(4, rev*0.4);
			}).add;

			SynthDef(\delay, {
				arg inBus, freq, atk=0.1, rel=0.5, maxdel=1.0, deltime=0.3, decay=0.5, level=0.5, gate=1;
				var delay, input, env, klick;
				env = EnvGen.kr(Env.asr(atk, 1, rel), gate, doneAction:2);
				input = In.ar(inBus) * level * env;
				delay = CombL.ar(input, maxdel, deltime, decay);
				Out.ar(freq, delay);
			}).add;

			SynthDef(\pannerCircle, {
				arg inBus, outBus, speed=0.01, level=1, dir=1, width=2;
				var pan, input;
				input = In.ar(inBus) * level;
				pan = PanAz.ar(4, input, LFSaw.kr(speed) * dir, width: width);
				Out.ar(outBus, pan);
			}).add;

			SynthDef(\discreteOut, {
				arg inBus, freq, gate=1;
				var input, env;
				env = EnvGen.kr(Env.asr(), gate, doneAction: 2);
				input = In.ar(inBus) * env;
				Out.ar(freq, input);
			}).add;

			SynthDef(\outSynth, {
				arg inBus, outBus, gate=1;
				var input, signal, env;
				env = EnvGen.kr(Env.asr(), gate, doneAction: 2);
				input = In.ar(inBus, 1) * env;
				signal = input;
				signal = Splay.ar(signal);
				Out.ar(outBus, signal);
			}).add;

			SynthDef(\noGator, {
				arg inBus, inBusV, outBus, outBusV, revOut, revVoxAmp=0.5, revInpAmp=0.7;
				var input, vocal;
				input = In.ar(inBus);
				vocal = In.ar(inBusV);
				Out.ar(outBus, input);
				Out.ar(outBusV, vocal);
				Out.ar(revOut, (input*revInpAmp + vocal*revVoxAmp));
			}).add;


			SynthDef(\master, {
				arg inBus, level=0.8;
				var sig;
				sig = In.ar(inBus, 8);
				sig = Limiter.ar(sig, level);
				Out.ar(0, sig);
			}).add;
		)
	}
}